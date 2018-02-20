/*
 * Copyright (C) 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.rpader;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.*;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordCount {

    private static final Logger LOGGER = LoggerFactory.getLogger(WordCount.class);

    private static class CleanTrimFn extends DoFn<String, String> {

        @ProcessElement
        public void processElement(ProcessContext c, BoundedWindow w) throws Exception {
            LOGGER.info("Cleaning strings");
            String s = c.element();
            if (s.startsWith("[")) {
                s = s.substring(1);
            }
            if (s.endsWith("]")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(",")) {
                s = s.substring(0, s.length() - 1);
            }
            c.output(s);
        }
    }

    public interface WordCountOptions extends PipelineOptions {
        @Description("Path of the file to read from")
        @Validation.Required
        ValueProvider<String> getInputFile();

        void setInputFile(ValueProvider<String> value);

    }

    public static void main(String[] args) {
        LOGGER.info("Initializing options");
        WordCountOptions options = PipelineOptionsFactory.fromArgs(args).withValidation()
                .as(WordCountOptions.class);

        LOGGER.info("Creating pipeline");
        Pipeline p = Pipeline.create(options);
        LOGGER.info("Starting...");
        p.apply(TextIO.read().from(options.getInputFile()))
                .apply(ParDo.of(new CleanTrimFn()))
                .apply(PubsubIO.writeStrings().to("projects/rmp-sandbox/topics/bb-new-product"));
        p.run();
    }


}
