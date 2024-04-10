Releasing the Elm Plugin to the JetBrains Marketplace
=====================================================

To build the `.zip` file for distribution run:

```bash
./gradlew buildPlugin
```

If all went well the `.zip` file can be found in `build/distributions/`

The `.zip` file may be uploaded to the JetBrains Marketplace using the `Update` button by someone with sufficient rights. 

Automatic releases with GitHub's CI/CD are being worked on.