package io.github.tax1116.fixedwidthcodec.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import io.github.tax1116.fixedwidthcodec.annotations.Align
import io.github.tax1116.fixedwidthcodec.annotations.ArrayField
import io.github.tax1116.fixedwidthcodec.annotations.MetaField
import io.github.tax1116.fixedwidthcodec.annotations.MetaField.Companion.EOL
import io.github.tax1116.fixedwidthcodec.annotations.ObjectField

internal object SerializeFunctionBuilder {
    private const val FUNCTION_NAME = "serialize"

    fun build(declaration: KSClassDeclaration, props: List<KSPropertyDeclaration>): FunSpec = FunSpec.builder(FUNCTION_NAME)
        .addParameter(
            ParameterSpec.builder(
                name = "record",
                type = ClassName(declaration.packageName.asString(), declaration.simpleName.asString()),
            ).build(),
        )
        .returns(ByteArray::class)
        .addCode(createSerializeStatement(props))
        .build()

    @OptIn(KspExperimental::class)
    private fun createSerializeStatement(props: List<KSPropertyDeclaration>): CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()

        codeBlockBuilder.addStatement("val sb = StringBuilder()")

        props.forEach { prop ->
            val fieldName = prop.simpleName.asString()
            val fieldType = prop.type.resolve()

            val arrayField = prop.getAnnotationsByType(ArrayField::class).firstOrNull()

            arrayField?.let {
                buildArrayFieldSerializeFunction(codeBlockBuilder, fieldName, fieldType, it)
                return@forEach
            }

            val objectField = prop.getAnnotationsByType(ObjectField::class).firstOrNull()
            objectField?.let {
                codeBlockBuilder.addStatement(
                    "sb.append(%T.$FUNCTION_NAME(record.%L).toString(charset))",
                    fieldType.toRecordMapperClassName(),
                    fieldName,
                )
                return@forEach
            }

            val metaField = prop.getAnnotationsByType(MetaField::class).firstOrNull()
                ?: throw IllegalStateException("MetaField annotation not found on $fieldName")

            if (metaField.length == EOL && props.last() != prop) {
                throw IllegalStateException("EOL-length field must be declared last ($fieldName)")
            }

            if (metaField.length == EOL && fieldType.declaration.simpleName.asString() != "String") {
                throw IllegalStateException("EOL-length field must be a String ($fieldName)")
            }

            codeBlockBuilder.addStatement(
                "sb.append($FUNCTION_NAME(record.%L, %L, %L, %T.%L, %S).toString(charset))",
                fieldName,
                metaField.length,
                charLiteral(metaField.paddingChar),
                Align::class,
                metaField.align.name,
                metaField.pattern,
            )
        }

        codeBlockBuilder.addStatement("return sb.toString().toByteArray(charset)")
        return codeBlockBuilder.build()
    }

    private fun buildArrayFieldSerializeFunction(
        codeBlockBuilder: CodeBlock.Builder,
        fieldName: String,
        fieldType: KSType,
        arrayField: ArrayField,
    ) {
        val typeArgument = fieldType.arguments.firstOrNull()?.type?.resolve()
            ?: throw IllegalStateException("ArrayField $fieldName must have a type argument")
        codeBlockBuilder.beginControlFlow("record.%L.forEach {", fieldName)

        if (typeArgument.isPrimitiveOrString()) {
            codeBlockBuilder.addStatement(
                "sb.append($FUNCTION_NAME(it, %L, %L, %T.%L, %S).toString(charset))",
                arrayField.elementLength,
                charLiteral(arrayField.elementPaddingChar),
                Align::class,
                arrayField.elementAlign.name,
                arrayField.elementPattern,
            )
            codeBlockBuilder.endControlFlow()
            return
        }
        codeBlockBuilder.addStatement(
            "sb.append(%T.$FUNCTION_NAME(it).toString(charset))",
            typeArgument.toRecordMapperClassName(),
        )
        codeBlockBuilder.endControlFlow()
    }
}
