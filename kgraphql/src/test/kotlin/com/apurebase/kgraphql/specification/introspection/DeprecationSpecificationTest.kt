package com.apurebase.kgraphql.specification.introspection

import com.apurebase.kgraphql.defaultSchema
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.expect
import com.apurebase.kgraphql.extract
import com.apurebase.kgraphql.schema.SchemaException
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class DeprecationSpecificationTest {

    @Test
    fun `queries may be deprecated`() {
        val expected = "sample query"
        val schema = defaultSchema {
            query("sample") {
                deprecate(expected)
                resolver<String> { "SAMPLE" }
            }
        }

        val response =
            deserialize(schema.executeBlocking("{__schema{queryType{fields(includeDeprecated: true){name, deprecationReason, isDeprecated}}}}"))
        assertThat(response.extract("data/__schema/queryType/fields[0]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__schema/queryType/fields[0]/isDeprecated"), equalTo(true))
    }

    @Test
    fun `mutations may be deprecated`() {
        val expected = "sample mutation"
        val schema = defaultSchema {
            mutation("sample") {
                deprecate(expected)
                resolver<String> { "SAMPLE" }
            }
        }

        val response =
            deserialize(schema.executeBlocking("{__schema{mutationType{fields(includeDeprecated: true){name, deprecationReason, isDeprecated}}}}"))
        assertThat(response.extract("data/__schema/mutationType/fields[0]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__schema/mutationType/fields[0]/isDeprecated"), equalTo(true))
    }

    data class Sample(val content: String)

    @Test
    fun `kotlin field may be deprecated`() {
        val expected = "sample type"
        val schema = defaultSchema {
            query("sample") {
                resolver<String> { "SAMPLE" }
            }

            type<Sample> {
                Sample::content.configure {
                    deprecate(expected)
                }
            }
        }

        val response =
            deserialize(schema.executeBlocking("{__type(name: \"Sample\"){fields(includeDeprecated: true){isDeprecated, deprecationReason}}}"))
        assertThat(response.extract("data/__type/fields[0]/deprecationReason/"), equalTo(expected))
        assertThat(response.extract("data/__type/fields[0]/isDeprecated/"), equalTo(true))
    }

    @Test
    fun `extension field may be deprecated`() {
        val expected = "sample type"
        val schema = defaultSchema {
            query("sample") {
                resolver<String> { "SAMPLE" }
            }

            type<Sample> {
                property("add") {
                    deprecate(expected)
                    resolver { (content) -> content.uppercase() }
                }
            }
        }

        val response =
            deserialize(schema.executeBlocking("{__type(name: \"Sample\"){fields(includeDeprecated: true){name, isDeprecated, deprecationReason}}}"))
        assertThat(response.extract("data/__type/fields[1]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__type/fields[1]/isDeprecated"), equalTo(true))
    }

    @Suppress("unused")
    enum class SampleEnum { ONE, TWO, THREE }

    @Test
    fun `enum value may be deprecated`() {
        val expected = "some enum value"
        val schema = defaultSchema {
            query("sample") {
                resolver<String> { "SAMPLE" }
            }

            enum<SampleEnum> {
                value(SampleEnum.ONE) {
                    deprecate(expected)
                }
            }
        }

        val response =
            deserialize(schema.executeBlocking("{__type(name: \"SampleEnum\"){enumValues(includeDeprecated: true){name, isDeprecated, deprecationReason}}}"))
        assertThat(response.extract("data/__type/enumValues[0]/name"), equalTo(SampleEnum.ONE.name))
        assertThat(response.extract("data/__type/enumValues[0]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__type/enumValues[0]/isDeprecated"), equalTo(true))
    }

    @Test
    fun `optional input value may be deprecated`() {
        data class InputType(val oldOptional: String?, val new: String)

        val expected = "deprecated input value"
        val schema = defaultSchema {
            inputType<InputType> {
                InputType::oldOptional.configure {
                    deprecate(expected)
                }
            }
        }

        val response =
            deserialize(schema.executeBlocking("{__type(name: \"InputType\"){inputFields(includeDeprecated: true){name, deprecationReason, isDeprecated}}}"))
        assertThat(response.extract("data/__type/inputFields[0]/name"), equalTo("new"))
        assertThat(response.extract("data/__type/inputFields[0]/deprecationReason"), equalTo(null))
        assertThat(response.extract("data/__type/inputFields[0]/isDeprecated"), equalTo(false))
        assertThat(response.extract("data/__type/inputFields[1]/name"), equalTo("oldOptional"))
        assertThat(response.extract("data/__type/inputFields[1]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__type/inputFields[1]/isDeprecated"), equalTo(true))
    }

    @Test
    fun `required input value may not be deprecated`() {
        data class InputType(val oldRequired: String, val new: String)

        expect<SchemaException>("Required fields cannot be marked as deprecated") {
            defaultSchema {
                inputType<InputType> {
                    InputType::oldRequired.configure {
                        deprecate("deprecated input value")
                    }
                }
            }
        }
    }

    @Test
    fun `deprecated input values should not be returned by default`() {
        data class InputType(val oldOptional: String?, val new: String)

        val expected = "deprecated input value"
        val schema = defaultSchema {
            inputType<InputType> {
                InputType::oldOptional.configure {
                    deprecate(expected)
                }
            }
        }

        val response =
            deserialize(schema.executeBlocking("{__type(name: \"InputType\"){inputFields{name, deprecationReason, isDeprecated}}}"))
        assertThat(response.extract("data/__type/inputFields[0]/name"), equalTo("new"))
        assertThat(response.extract("data/__type/inputFields[0]/deprecationReason"), equalTo(null))
        assertThat(response.extract("data/__type/inputFields[0]/isDeprecated"), equalTo(false))
        // oldOptional should not be returned
        assertThat(response.contains("data/__type/inputFields[1]"), equalTo(false))
    }
}
