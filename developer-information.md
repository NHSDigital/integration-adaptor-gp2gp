### Releasing a new version to Docker Hub

First identify which is the most recent commit within GitHub which contains only changes which are marked as Done within Jira.
You can also review what commits have gone in by using the git log command or IDE.

Make a note of the most recent Release within GitHub, and identify what the next version number to use will be.

On the repository home page, navigate to **Releases** and make sure the latest release is on screen. Click on the `Draft a new release` button

To create a new release within GitHub, specify a tag version to use (e.g. 0.11), and the target being the latest commit using the options available.
Click on the `Generate release notes` button and this will list all the current changes from the recent commit.

Log into **Docker Desktop** using the credentials stored within our AWS accounts Secrets Manager, secret name `nhsdev-dockerhub-credentials` in London region.
Go to `AWS Management Console > Service Manager` then find the option `retrieve keys`


Now build the adaptor using the following commands:

```shell
git fetch
git checkout <version tag>
```
Replace `<version tag>` with the version tag above. (e.g. 0.11)

When running the **buildx** commands you may get an error asking you to run the following command, which you should do.
```shell
docker buildx create --use
```

Replace `<version>` with the version tag above. (e.g. nhsdev/nia-gp2gp-adaptor:0.11)

_NOTE_ that the command may take up to 20+ minutes.

```shell
docker buildx build -f docker/service/Dockerfile . --platform linux/arm64/v8,linux/amd64 --tag nhsdev/nia-gp2gp-adaptor:<version> --push
```