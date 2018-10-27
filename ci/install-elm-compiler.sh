#!/usr/bin/env bash

wget "https://github.com/elm/compiler/releases/download/0.19.0/binaries-for-linux.tar.gz"
tar zxvf binaries-for-linux.tar.gz
sudo mv elm /usr/local/bin/
elm --version