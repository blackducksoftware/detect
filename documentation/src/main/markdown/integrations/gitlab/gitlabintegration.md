# [detect_product_short] GitLab Integration
[detect_product_long] is designed to run in the native build environment of the project you want to scan. The following procedures provide guidance on setting up [detect_product_short] with your GitLab continuous integration builds.

## Configuring with API tokens
The recommended way of configuring [detect_product_short] from a GitLab pipeline is to use an API token. This is detailed as follows.

1. In [bd_product_short], navigate to the profile of the user whose credentials are used to scan projects from the pipeline.
2. Scroll down to the **User Access Token** section, and complete the fields to create a new token.
3. Check both the **Read Access** and **Write Access** boxes.
4. Click **Generate.** Save or copy the displayed token.

    <figure>
    <img src="../gitlab/images/myaccesstokens.png"
         alt="Creating an access token">
    <figcaption>Creating the access token</figcaption>
    </figure>

## Configuring your environment variables
1. In the sidebar, navigate to **Settings**. Then select **CI/CD**.

2. Expand the **Secret variables** tab.  

    <figure>
    <img src="../gitlab/images/pipelineconfig1.png"
         alt="Configuring the pipeline secrets">
    <figcaption>Configuring the pipeline secrets</figcaption>
    </figure>

3. Create two environment variables:

	- BLACKDUCK\_URL - URL of your [bd_product_short] installation.

	- BLACKDUCK\_TOKEN - API token that you generated in [bd_product_short].

	<note type="note">You can make these variables protected. For additional information, refer to [Gitlab protected secret variables](https://gitlab.com/help/ci/variables/README#protected-secret-variables).</note>

4. Configure [detect_product_short] to be a script step in the *.gitlab-ci.yml* file of the project you want to scan. Then add the snippet for [detect_product_short].    

    Ensure that the final line of the following command fits on a single command line.

    ```
	image: java:8build:
		stage: build
		script:
		- ./gradlew assemble
	test:
		stage: test
		script:
			- bash <(curl -s -L https://detect.blackduck.com/detect9.sh) --blackduck.url="${BLACKDUCK\_URL}" --blackduck.api.token="${BLACKDUCK\_TOKEN}" --blackduck.trust.cert=true --<any other flags>
    ```

5. Configure [detect_product_short] as a script build step so GitLab can enforce build changes influenced by [detect_product_short]. For example, checking for policy, failing builds according to policy, and others.

6. After you commit the change to *.gitlab-ci.yml,* the pipeline runs. After the build with [detect_product_short] completes, you can view the complete scan results in your [bd_product_short] instance.

## Configuring with username and password
For improved security, it is recommended to use a revocable API token, as described in the preceding process, instead of storing an account password in GitLab settings.

1. In the sidebar project menu, navigate to **Settings** Then select **CI/CD**.

2. Expand the **Secret variables** tab.  

    <figure>
    <img src="../gitlab/images/pipelineconfig2.png"
         alt="Configuring pipeline secret variables">
    <figcaption>Configuring the pipeline secret variables</figcaption>
    </figure>

3. Create three environment variables:

	- BLACKDUCK\_URL - URL of your [bd_product_short] installation.

	- BLACKDUCK\_USERNAME - containing the username of the [bd_product_short] account to be used.

	- BLACKDUCK\_PASSWORD - containing the password of the [bd_product_short] account to be used.

	<note type="note">You can make these variables protected. For additional information, refer to [Gitlab protected secret variables](https://docs.gitlab.com/ee/ci/variables/#protect-a-cicd-variable).</note>

4. Configure [detect_product_short] to be a script step in the *.gitlab-ci.yml* file of the project you want to scan. Then add the snippet for [detect_product_short].    

    Ensure that the final line of the following command fits on a single command line.

    ```
	image: java:8build:
		stage: build
		script:
		- ./gradlew assemble
	test:
		stage: test
		script:
			- bash <(curl -s -L <https://detect.blackduck.com/detect9.sh>) --blackduck.url="${BLACKDUCK\_URL}" --blackduck.hub.username="${BLACKDUCK\_USERNAME}" --blackduck.hub.password="${BLACKDUCK\_PASSWORD}" --blackduck.trust.cert=true --<any other flags>
    ```

5. Configure [detect_product_short] as a script build step so GitLab can enforce build changes influenced by [detect_product_short]. For example, checking for policy, failing builds according to policy, and others.

6. After you commit the change to *gitlab-ci.yml*, the pipeline runs. When the build with [detect_product_short] completes, you can view the scan results in your [bd_product_short] instance.

