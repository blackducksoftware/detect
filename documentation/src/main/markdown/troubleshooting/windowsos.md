# Windows OS hints for [detect_product_short]

## Passing spaces in arguments
* Windows considers space as a separator for arguments and discards all the values which would be within double quotes. Eg: If you pass --detect.project.name=" Windows Project ", then that is being interpreted as "--detect.project.name=" "Windows" "Project". To pass spaces inside an argument in Windows OS, then you can either use single quotes ('single') or you can use backtick character (`) to escape spaces.

## Using multi-byte characters
* Using Windows with multibyte characters for different languages such as Korean or Japanese, you must configure Windows by using the chcp command to change the character code for cmd shell.