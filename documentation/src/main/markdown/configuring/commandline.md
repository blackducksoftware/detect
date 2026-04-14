# On the command line

One method for configuring [detect_product_short] is by setting [property values](../properties/all-properties.md) on the command line.
When setting a property value on the command line, prefix the property name with two hyphens (--).

To add one property setting to the command line, add the following at the end:
```
{space}--{property name}={value}
```
There is a space before and between each complete property setting, but there are no spaces around the equals sign (=).

For example,
to set property *detect.project.name*:
```
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) --detect.project.name=MyProject
```
