package io.github.tax1116.fixedwidthcodec.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.tax1116.fixedwidthcodec.annotations.Align
import io.github.tax1116.fixedwidthcodec.annotations.ArrayField
import io.github.tax1116.fixedwidthcodec.annotations.ArrayFieldType
import io.github.tax1116.fixedwidthcodec.annotations.MetaField
import io.github.tax1116.fixedwidthcodec.annotations.ObjectField
import io.github.tax1116.fixedwidthcodec.processor.FixedWidthRecordMapperProcessor.Companion.RECORD_POSTFIX

internal object DeserializeFunctionBuilder {
    private const val FUNCTION_NAME = "deserialize"

    fun build(declaration: KSClassDeclaration, props: List<KSPropertyDeclaration>): FunSpec = FunSpec.builder(FUNCTION_NAME)
        .addParameter(ParameterSpec.builder("bytes", ByteArray::class).build())
        .addParameters(buildSizeParameterSpec(props))
        .returns(declaration.toClassName())
        .addCode(createDeserializeStatement(declaration, props))
        .build()

    @OptIn(KspExperimental::class)
    private fun createDeserializeStatement(declaration: KSClassDeclaration, props: List<KSPropertyDeclaration>): CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()
        codeBlockBuilder.addStatement("var cursor = 0")

        props.forEach { prop ->
            val fieldName = prop.simpleName.asString()
            val fieldType = prop.type.resolve()
            val fieldTypeName = fieldType.declaration.simpleName.asString()
            val fieldPackage = fieldType.declaration.packageName.asString()

            val arrayField = prop.getAnnotationsByType(ArrayField::class).firstOrNull()
            arrayField?.let {
                buildArrayFieldDeserializeFunction(codeBlockBuilder, fieldName, fieldType, it)
                return@forEach
            }

            val objectField = prop.getAnnotationsByType(ObjectField::class).firstOrNull()
            objectField?.let {
                codeBlockBuilder.addStatement(
                    "val %L = %T.$FUNCTION_NAME(slice(bytes, cursor, cursor + %L))",
                    fieldName,
                    ClassName(fieldPackage, "$fieldTypeName$RECORD_POSTFIX"),
                    objectField.length,
                )
                codeBlockBuilder.addStatement("cursor += %L", objectField.length)
                return@forEach
            }

            val metaField = prop.getAnnotationsByType(MetaField::class).firstOrNull()
                ?: throw IllegalStateException("MetaField annotation not found on $fieldName")

            if (metaField.length == MetaField.EOL && props.last() != prop) {
                throw IllegalStateException("EOL-length field must be declared last ($fieldName)")
            }

            if (metaField.length == MetaField.EOL) {
                codeBlockBuilder.addStatement(
                    "val %L = bytesTo$fieldTypeName(bytes, cursor, bytes.size, %L, %T.%L)",
                    fieldName,
                    charLiteral(metaField.paddingChar),
                    Align::class,
                    metaField.align.name,
                )
                return@forEach
            }

            codeBlockBuilder.addStatement(
                "val %L = bytesTo$fieldTypeName(bytes, cursor, cursor + %L, %L, %T.%L)",
                fieldName,
                metaField.length,
                charLiteral(metaField.paddingChar),
                Align::class,
                metaField.align.name,
            )
            codeBlockBuilder.addStatement("cursor += %L", metaField.length)
        }

        codeBlockBuilder.addStatement(
            "return %T(${props.joinToString(", ") { it.simpleName.asString() }})",
            declaration.toClassName(),
        )

        return codeBlockBuilder.build()
    }

    private fun buildArrayFieldDeserializeFunction(
        codeBlockBuilder: CodeBlock.Builder,
        fieldName: String,
        fieldType: KSType,
        arrayField: ArrayField,
    ) {
        val typeArgument = fieldType.arguments.firstOrNull()?.type?.resolve()
            ?: throw IllegalStateException("ArrayField $fieldName must have a type argument")

        codeBlockBuilder.addStatement("val %L = mutableListOf<%T>()", fieldName, typeArgument.toClassName())
        val size = when (arrayField.type) {
            ArrayFieldType.FIXED -> arrayField.size.toString()
            ArrayFieldType.IN_FIELD -> arrayField.sizeField.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("ArrayField $fieldName has IN_FIELD type but sizeField is blank")
            ArrayFieldType.PARAMETER -> "${fieldName}Size"
        }
        codeBlockBuilder.beginControlFlow("repeat($size)")

        if (typeArgument.isPrimitiveOrString()) {
            val typeName = typeArgument.declaration.simpleName.asString()
            codeBlockBuilder.addStatement(
                "%L.add(bytesTo$typeName(bytes, cursor, cursor + %L, %L, %T.%L))",
                fieldName,
                arrayField.elementLength,
                charLiteral(arrayField.elementPaddingChar),
                Align::class,
                arrayField.elementAlign.name,
            )
            codeBlockBuilder.addStatement("cursor += %L", arrayField.elementLength)
            codeBlockBuilder.endControlFlow()
            return
        }

        codeBlockBuilder.addStatement(
            "%L.add(%T.$FUNCTION_NAME(slice(bytes, cursor, cursor + %L)))",
            fieldName,
            typeArgument.toRecordMapperClassName(),
            arrayField.elementLength,
        )
        codeBlockBuilder.addStatement("cursor += %L", arrayField.elementLength)
        codeBlockBuilder.endControlFlow()
    }

    /**
     * If any primary-constructor property uses [ArrayField] with [ArrayFieldType.PARAMETER],
     * the generated `deserialize` function receives an extra `<fieldName>Size: Int` parameter
     * so the caller can supply the count at runtime.
     */
    @OptIn(KspExperimental::class)
    private fun buildSizeParameterSpec(props: List<KSPropertyDeclaration>): List<ParameterSpec> {
        val arrayFieldProps = props.filter {
            it.isAnnotationPresent(ArrayField::class) &&
                it.getAnnotationsByType(ArrayField::class).first().type == ArrayFieldType.PARAMETER
        }

        return arrayFieldProps.map {
            val fieldName = it.simpleName.asString()
            ParameterSpec.builder("${fieldName}Size", Int::class).build()
        }
    }
}
