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

import opennlp.tools.util.ParamChecks;

/**
 * One Hunspell morphological field. Tags omit the colon; an empty tag denotes raw text.
 *
 * @param tag The tag, or an empty string for untagged text. Must not be {@code null}.
 * @param value The field value. Must not be {@code null}.
 */
public record HunspellField(String tag, String value) {

  /**
   * Creates a field.
   *
   * @throws IllegalArgumentException if either argument is {@code null}.
   */
  public HunspellField {
    ParamChecks.requireNonNullArg(tag, "tag");
    ParamChecks.requireNonNullArg(value, "value");
  }

  /** Parses a tagged or raw dictionary field without splitting its value. */
  static HunspellField parse(String text) {
    final int colon = text.indexOf(':');
    return colon <= 0 ? new HunspellField("", text)
        : new HunspellField(text.substring(0, colon), text.substring(colon + 1));
  }

  /** {@return the field in Hunspell's text layout} */
  @Override
  public String toString() {
    return tag.isEmpty() ? value : tag + ":" + value;
  }
}
