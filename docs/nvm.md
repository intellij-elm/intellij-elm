# NVM

## Overview
Some of the external tools used by this plugin (e.g. [elm-format](https://github.com/avh4/elm-format) and
[elm-test](https://github.com/elm-explorations/test)) rely on [node](https://nodejs.org). In order for the plugin to
fully function, it needs to be able to execute `node` from within the IntelliJ environment. In most cases it will be able
to do this because when `node` is installed, it is put on the system path. However, if `node` is installed using `nvm`
then depending on how IntelliJ is launched, this might not be the case.


## Symptoms
If this occurs then on the plugin's Settings screen, the _Auto Discover_ buttons might be unable to find the location of
`elm-format` or `elm-test`, or, if a path is manually entered, an error such as the following might be shown:

    /usr/bin/env: 'node': No such file or directory


## Cause
The reason for this is because the way `nvm` adds `node` to the path is by making some updates to the profile (e.g.
`~/.bash_profile`, `~/.zshrc`, `~/.profile`, or `~/.bashrc`), as explained
[here](https://github.com/nvm-sh/nvm#install--update-script). If, for example, it makes these changes to `~/.bashrc`,
`node` will only be on the path when that file is processed, i.e. when a bash shell is started. In this situation, if
IntelliJ is launched without the use of a bash shell, it will result in `node` not being on the path within the IntelliJ
environment. One example of this is if IntelliJ is launched from a `.desktop` file, e.g. if it was installed by
[snap](https://snapcraft.io/).


## Solution
To resolve the problem above, IntelliJ needs to be launched in such a way that `node` will be on the path. One simple
solution is to manually launch it from within a shell which has already processed the relevant configuration file, e.g.
from within a bash shell, if `.bashrc` was where the `nvm` install updated the path.

If launching IntelliJ from within a `.desktop` file a similar change can be made, which is to update the `Exec` line so
that instead of directly launching IntelliJ, it instead launches a shell such as bash, and gets it to then launch
IntelliJ:

    Exec=/bin/bash -ic "/snap/bin/intellij-idea-ultimate %f" 

Note that in this example, the `-ic` argument is important, specifically the `i` flag: this launches bash in
[interactive mode](https://www.gnu.org/software/bash/manual/html_node/What-is-an-Interactive-Shell_003f.html#What-is-an-Interactive-Shell_003f),
which is what causes bash to process `.bashrc`, as explained [here](https://www.gnu.org/software/bash/manual/html_node/Bash-Startup-Files.html)

