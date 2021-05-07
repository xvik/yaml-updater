package ru.vyarus.yaml.config.updater.parse.comments

import ru.vyarus.yaml.config.updater.parse.comments.util.MultilineValue
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 07.05.2021
 */
class MultilineDetectionTest extends Specification {

    def "Check recognition"() {

        expect:
        println source
        def res = MultilineValue.detect(source)
        res?.keep == keep
        res?.ending == end
        res?.indent == pad

        where:
        source       | keep  | end  | pad
        "sample"     | null  | null | null
        "|"          | true  | 0    | -1
        ">"          | false | 0    | -1
        "|+"         | true  | 1    | -1
        ">-"         | false | -1   | -1
        "|2"         | true  | 0    | 2
        ">+2"        | false | 1    | 2
        "| #comment" | true  | 0    | -1
    }
}
