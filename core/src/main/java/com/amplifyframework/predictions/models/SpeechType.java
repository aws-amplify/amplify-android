/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.predictions.models;

/**
 * Different types of parts of speech.
 *
 * @see <a href=https://docs.aws.amazon.com/comprehend/latest/dg/how-syntax.html>Analyze Syntax</a>
 */
public enum SpeechType {
    /**
     * Words that typically modify nouns.
     * e.g. 'yellow', 'quick'
     */
    ADJECTIVE,

    /**
     * The head of a prepositional or postpositional phrase.
     * e.g. 'with', 'over', 'to'
     */
    ADPOSITION,

    /**
     * Words that typically modify verbs. They may also modify
     * adjectives and other adverbs.
     * e.g. 'easily', 'well'
     */
    ADVERB,

    /**
     * Function words that accompanies the verb of a verb phrase.
     * e.g. 'can', 'must', 'will'
     */
    AUXILIARY,

    /**
     * Words that links words or phrases without subordinating
     * one to the other.
     * e.g. 'and', 'but', 'so'
     */
    COORDINATING_CONJUNCTION,

    /**
     * Articles and other words that specify a particular
     * noun phrase.
     * e.g. 'the', 'each', 'his'
     */
    DETERMINER,

    /**
     * Words used as an exclamation or part of an exclamation.
     * e.g. 'wow', 'yikes'
     */
    INTERJECTION,

    /**
     * Words that specify a person, place, thing, animal, or idea.
     * e.g. 'car', 'phone', 'tree'
     */
    NOUN,

    /**
     * Words, typically determiners, adjectives, or pronouns,
     * that express a number.
     * e.g. 'seven', '2020'
     */
    NUMERAL,

    /**
     * Function words associated with another word or phrase
     * to impart meaning.
     * e.g. 'up' in 'eat up', 'to' in 'to fly'
     */
    PARTICLE,

    /**
     * Words that substitute for nouns or noun phrases.
     * e.g. 'he', 'she', 'I'
     */
    PRONOUN,

    /**
     * A noun that is the name of a specific individual,
     * place or object.
     * e.g. 'AWS', 'John'
     */
    PROPER_NOUN,

    /**
     * Non-alphabetical characters that delimit text.
     * e.g. '.', ';', '?'
     */
    PUNCTUATION,

    /**
     * A conjunction that links parts of sentences by making
     * one of them part of the other.
     * e.g. 'after', 'because', 'if'
     */
    SUBORDINATING_CONJUNCTION,

    /**
     * Word-like entities such as mathematical symbols.
     * e.g. '$', '%', '='
     */
    SYMBOL,

    /**
     * Words that signal events and actions.
     * e.g. 'run', 'eat', 'sleep'
     */
    VERB,

    /**
     * Words that can't be assigned a part of speech category.
     */
    UNKNOWN
}
