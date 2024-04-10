# Lamdera project support

This plugin supports Lamdera projects.
A Lamdera project currently consists of:
- `elm.json` with a direct dependency to packages "lamdera/codecs" and "lamdera/core"
- `Frontend.elm` with a top-level definition named `app` defined by [frontend](https://dashboard.lamdera.app/docs/api) 
- `Backend.elm` with a top-level definition named `app` defined by [backend](https://dashboard.lamdera.app/docs/api)

To get going, you can download Lamdera's compiler here:
[Download lamdera executable](https://dashboard.lamdera.app/docs/download)


## Tips for using `lamdera` executable as a compiler replacement for `elm`

To correctly set up the local package path for Lamdera, you can do:

`ln -s ~/.elm/0.19.1 ~/.elm/0.19.1-1.0.1`

This plugin recognizes the Lamdera executable as a replacement for the elm executable in the plugin's settings:

![](../assets/plugin-settings-lamda.png)


### Known quirks

On some Linux systems, the message `libtinfo.so.5: no version information available (required by lamdera)` appears with every `lamdera` command.
There seems to be no easy solution, but until now, Lamdera works as expected despite this message. This plugin will skip this message when analyzing Lamdera's compiler output.
