<#ftl output_format="Markdown">

# All Properties

This page lists all [detect_product_long] properties, including advanced configuration and those properties that have been deprecated.
For most use cases, refer to [basic properties](basic-properties.md).

<#list groups?sort_by("groupName") as group>

## [${group.groupName}](<#noautoesc>${group.location}.md</#noautoesc>)

| Property | Description |
| --- | --- |
<#list group.simple![] as option>
<#noautoesc>
| [${option.propertyKey}](${option.location}){.section .ft-secondary-toc} | <#if option.defaultValue?has_content><p>default: ${option.defaultValue}</p></#if><#if option.hasAcceptableValues> <p>Acceptable Values: ${option.acceptableValues?join(", ")} </p></#if><p><#if option.propertyName?has_content>${option.propertyName}: </#if>${option.description} </p> <#if option.deprecated><p>**DEPRECATED: ${option.deprecatedDescription!"This property is deprecated."} This property will be removed in ${option.deprecatedRemoveInVersion}.**</p></#if><#if option.deprecatedValues?has_content><p>**DEPRECATED VALUES:** <#list option.deprecatedValues as deprecatedValue> ${deprecatedValue.value}: ${deprecatedValue.reason}</#list></p></#if> |
</#noautoesc>
</#list>
<#list group.advanced![] as option>
<#noautoesc>
| [${option.propertyKey}](${option.location}){.section .ft-secondary-toc} (Advanced)| <#if option.defaultValue?has_content><p>default: ${option.defaultValue}</p></#if><#if option.hasAcceptableValues> <p>Acceptable Values: ${option.acceptableValues?join(", ")} </p></#if><p><#if option.propertyName?has_content>${option.propertyName}: </#if>${option.description} </p> <#if option.deprecated><p>**DEPRECATED: ${option.deprecatedDescription!"This property is deprecated."} This property will be removed in ${option.deprecatedRemoveInVersion}.**</p></#if><#if option.deprecatedValues?has_content><p>**DEPRECATED VALUES:** <#list option.deprecatedValues as deprecatedValue> ${deprecatedValue.value}: ${deprecatedValue.reason}</#list></p></#if> |
</#noautoesc>
</#list>
<#list group.deprecated![] as option>
<#noautoesc>
| [${option.propertyKey}](${option.location}){.section .ft-secondary-toc} (Deprecated)| <#if option.defaultValue?has_content><p>default: ${option.defaultValue}</p></#if><#if option.hasAcceptableValues> <p>Acceptable Values: ${option.acceptableValues?join(", ")} </p></#if><p><#if option.propertyName?has_content>${option.propertyName}: </#if>${option.description} </p> <#if option.deprecated><p>**DEPRECATED: ${option.deprecatedDescription!"This property is deprecated."} This property will be removed in ${option.deprecatedRemoveInVersion}.**</p></#if><#if option.deprecatedValues?has_content><p>**DEPRECATED VALUES:** <#list option.deprecatedValues as deprecatedValue> ${deprecatedValue.value}: ${deprecatedValue.reason}</#list></p></#if> |
</#noautoesc>
</#list>

</#list>

Documentation version: ${program_version}
