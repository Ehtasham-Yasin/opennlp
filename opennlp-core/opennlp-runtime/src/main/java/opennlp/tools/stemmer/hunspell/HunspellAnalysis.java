/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import opennlp.tools.util.ParamChecks;

/**
 * An immutable morphological reading returned by {@link HunspellStemmer#analyze(CharSequence)}.
 * Entry and affix fields retain their application order. Flag fields ({@code fl:}) are
 * retained internally for text compatibility, but are omitted from {@link #fields()}.
 * Untagged prefix text and custom raw fields have an empty tag.
 */
public final class HunspellAnalysis {

  private final String stem;
  private final List<HunspellAnalysis> parts;
  private final List<HunspellField> fields;
  private final List<HunspellField> allFields;
  private final String surface;

  /**
   * Creates a reading, copying the supplied lists.
   *
   * @param stem The stem. Must not be {@code null}.
   * @param parts The ordered compound parts, empty for a simple word.
   * @param fields The ordered fields. Flag fields are retained only for text rendering.
   * @throws IllegalArgumentException if an argument or list element is {@code null}.
   */
  public HunspellAnalysis(String stem, List<HunspellAnalysis> parts, List<HunspellField> fields) {
    this(stem, parts, fields, stem);
  }

  private HunspellAnalysis(String stem, List<HunspellAnalysis> parts,
      List<HunspellField> fields, String surface) {
    ParamChecks.requireNonNullArg(stem, "stem");
    ParamChecks.requireNonNullArg(parts, "parts");
    ParamChecks.requireNonNullArg(fields, "fields");
    for (HunspellAnalysis part : parts) {
      ParamChecks.requireNonNullArg(part, "part");
    }
    for (HunspellField field : fields) {
      ParamChecks.requireNonNullArg(field, "field");
    }
    this.stem = stem;
    this.parts = List.copyOf(parts);
    this.allFields = List.copyOf(fields);
    this.fields = fields.stream().filter(field -> !"fl".equals(field.tag())).toList();
    this.surface = surface;
  }

  /** {@return the first {@code st:} value, or the entry spelling when absent;
   * for a compound, the concatenated part stems} */
  public String stem() {
    return stem;
  }

  /** {@return the ordered compound parts, or an empty immutable list for a simple word} */
  public List<HunspellAnalysis> parts() {
    return parts;
  }

  /** {@return the ordered fields, excluding {@code fl:} flags, as an immutable list} */
  public List<HunspellField> fields() {
    return fields;
  }

  /**
   * Finds all values for a tag in field order, preserving repeated tags.
   *
   * @param tag The tag without a colon, or an empty string for raw fields.
   * @return An immutable list, empty if the tag is absent.
   * @throws IllegalArgumentException if {@code tag} is {@code null}.
   */
  public List<String> fieldValues(String tag) {
    ParamChecks.requireNonNullArg(tag, "tag");
    return fields.stream().filter(field -> tag.equals(field.tag())).map(HunspellField::value).toList();
  }

  /** {@return this reading in Hunspell's field layout, including flag and compound fields} */
  public String toHunspellString() {
    final List<String> text = new ArrayList<>();
    for (HunspellAnalysis part : parts) {
      final String analysis = part.toHunspellString();
      text.add(HunspellDictionary.COMPOUND_PART_FIELD + part.surface
          + (analysis.isEmpty() ? "" : " " + analysis));
    }
    for (HunspellField field : allFields) {
      text.add(field.toString());
    }
    return String.join(" ", text);
  }

  /** Builds a simple reading directly from the dictionary's field list. */
  static HunspellAnalysis fromFields(String root, List<HunspellField> fields) {
    final String stem = fields.stream().filter(field -> "st".equals(field.tag()))
        .map(HunspellField::value).findFirst().orElse(root);
    return new HunspellAnalysis(stem, List.of(), fields);
  }

  /** Associates the surface spelling used in a compound's {@code pa:} field. */
  HunspellAnalysis withSurface(String text) {
    return new HunspellAnalysis(stem, parts, allFields, text);
  }

  /** Combines simple readings into a compound. */
  static HunspellAnalysis compound(List<HunspellAnalysis> parts) {
    final StringBuilder stem = new StringBuilder();
    for (HunspellAnalysis part : parts) {
      stem.append(part.stem());
    }
    return new HunspellAnalysis(stem.toString(), parts, List.of());
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object other) {
    return other instanceof HunspellAnalysis analysis && stem.equals(analysis.stem)
        && parts.equals(analysis.parts) && allFields.equals(analysis.allFields)
        && surface.equals(analysis.surface);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(stem, parts, allFields, surface);
  }
}
