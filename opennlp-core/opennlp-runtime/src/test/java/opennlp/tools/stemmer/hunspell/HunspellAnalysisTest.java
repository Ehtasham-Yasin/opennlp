/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package opennlp.tools.stemmer.hunspell;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HunspellAnalysisTest {

  private static HunspellStemmer stemmer(String affix, String words) throws IOException {
    return new HunspellStemmer(HunspellDictionary.load(
        new ByteArrayInputStream(affix.getBytes(StandardCharsets.UTF_8)),
        new ByteArrayInputStream(words.getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  void testTypedFieldsAndHomonyms() throws IOException {
    final List<HunspellAnalysis> readings = stemmer("SFX A Y 1\nSFX A 0 s . is:plural\n",
        "2\ncard/A po:noun po:common\ncard/A po:verb\n").analyze("cards");
    Assertions.assertEquals(2, readings.size());
    final HunspellAnalysis noun = readings.getFirst();
    Assertions.assertEquals("card", noun.stem());
    Assertions.assertTrue(noun.parts().isEmpty());
    Assertions.assertEquals(List.of("noun", "common"), noun.fieldValues("po"));
    Assertions.assertEquals(List.of("plural"), noun.fieldValues("is"));
    Assertions.assertTrue(noun.fieldValues("unknown").isEmpty());
    Assertions.assertEquals(List.of(new HunspellField("st", "card"),
        new HunspellField("po", "noun"), new HunspellField("po", "common"),
        new HunspellField("is", "plural")), noun.fields());
    Assertions.assertEquals(List.of("verb"), readings.get(1).fieldValues("po"));
  }

  @Test
  void testFlagsAndRawPrefix() throws IOException {
    final HunspellAnalysis reading = stemmer("PFX U Y 1\nPFX U 0 un .\n",
        "1\ndone/U\n").analyze("undone").getFirst();
    Assertions.assertEquals("done", reading.stem());
    Assertions.assertEquals(List.of("un"), reading.fieldValues(""));
    Assertions.assertTrue(reading.fieldValues("fl").isEmpty());
    Assertions.assertEquals("un st:done fl:U", reading.toHunspellString());
  }

  @Test
  void testColonInRawPrefix() throws IOException {
    final HunspellAnalysis reading = stemmer("PFX U Y 1\nPFX U 0 x:y .\n",
        "1\ndone/U\n").analyze("x:ydone").getFirst();
    Assertions.assertEquals(List.of("x:y"), reading.fieldValues(""));
    Assertions.assertEquals("x:y st:done fl:U", reading.toHunspellString());
  }

  @Test
  void testRawFieldsAndColonInValue() throws IOException {
    final HunspellAnalysis reading = stemmer("",
        "1\nentry\tst:root <BAR > po:noun:custom fl:A :raw\n").analyze("entry").getFirst();
    Assertions.assertEquals("root", reading.stem());
    Assertions.assertEquals(List.of("<BAR", ">", ":raw"), reading.fieldValues(""));
    Assertions.assertEquals(List.of("noun:custom"), reading.fieldValues("po"));
    Assertions.assertEquals("st:root <BAR > po:noun:custom fl:A :raw", reading.toHunspellString());
  }

  @Test
  void testCompoundParts() throws IOException {
    final HunspellAnalysis compound = stemmer(
        "COMPOUNDFLAG C\nCOMPOUNDMIN 2\nSFX A Y 1\nSFX A 0 s . is:plural\n",
        "2\nriver/C po:noun\nboat/CA po:noun\n").analyze("riverboats").getFirst();
    Assertions.assertEquals("riverboat", compound.stem());
    Assertions.assertTrue(compound.fields().isEmpty());
    Assertions.assertEquals(List.of("river", "boat"),
        compound.parts().stream().map(HunspellAnalysis::stem).toList());
    Assertions.assertEquals(List.of("plural"), compound.parts().get(1).fieldValues("is"));
    Assertions.assertEquals("pa:river st:river po:noun pa:boats st:boat po:noun is:plural",
        compound.toHunspellString());
  }

  @Test
  void testBareCompoundEnd() throws IOException {
    final HunspellAnalysis compound = stemmer("COMPOUNDFLAG C\nCOMPOUNDMIN 2\n",
        "2\nriver/C\nboat/C\n").analyze("riverboat").getFirst();
    Assertions.assertEquals("boat", compound.parts().get(1).stem());
    Assertions.assertTrue(compound.parts().get(1).fields().isEmpty());
    Assertions.assertEquals("pa:river st:river pa:boat", compound.toHunspellString());
  }

  @Test
  void testNestedBreaksAndCompounds() throws IOException {
    final HunspellAnalysis reading = stemmer("COMPOUNDFLAG C\nCOMPOUNDMIN 2\n",
        "3\nriver/C\nboat/C\nhouse/C\n").analyze("riverboat-house-boat").getFirst();
    Assertions.assertEquals(List.of("river", "boat", "house", "boat"),
        reading.parts().stream().map(HunspellAnalysis::stem).toList());
    Assertions.assertEquals("pa:river st:river pa:boat pa:house st:house pa:boat st:boat",
        reading.toHunspellString());
  }

  @Test
  void testImmutableValue() {
    final List<HunspellField> fields = new ArrayList<>(List.of(new HunspellField("st", "root")));
    final List<HunspellAnalysis> parts = new ArrayList<>();
    final HunspellAnalysis reading = new HunspellAnalysis("root", parts, fields);
    fields.clear();
    parts.add(reading);
    Assertions.assertEquals("st:root", reading.toHunspellString());
    Assertions.assertTrue(reading.parts().isEmpty());
    Assertions.assertThrows(UnsupportedOperationException.class, () -> reading.fields().clear());
    Assertions.assertThrows(UnsupportedOperationException.class, () -> reading.parts().clear());
    Assertions.assertThrows(UnsupportedOperationException.class, () -> reading.fieldValues("st").clear());
    final HunspellAnalysis equal = new HunspellAnalysis("root", List.of(),
        List.of(new HunspellField("st", "root")));
    Assertions.assertEquals(reading, equal);
    Assertions.assertEquals(reading.hashCode(), equal.hashCode());
    Assertions.assertNotEquals(reading, HunspellAnalysis.fromFields("root",
        List.of(new HunspellField("st", "root"), new HunspellField("fl", "A"))));
  }

  @Test
  void testNullArguments() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new HunspellField(null, "value"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new HunspellField("po", null));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new HunspellAnalysis(null, List.of(), List.of()));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new HunspellAnalysis("stem", null, List.of()));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new HunspellAnalysis("stem", List.of(), null));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new HunspellAnalysis("stem", Arrays.asList((HunspellAnalysis) null), List.of()));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new HunspellAnalysis("stem", List.of(), Arrays.asList((HunspellField) null)));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new HunspellAnalysis("stem", List.of(), List.of()).fieldValues(null));
  }
}
