# elm-test

Run [elm-test](https://github.com/elm-explorations/test) from within IntelliJ.

This is useful if you want the convenience of running `elm-test` from a GUI with green and red lights for each test, indicating success and failure respectively.

Enabling this feature has no significant performance implications beyond the work that `elm-test` itself does to run your tests.


## Enable

1. Open **IntelliJ Settings**
2. Select **Languages & Frameworks** from the left-side pane
3. Select **Elm**
4. Fill out the section titled **elm-test** (use the **Auto Discover** button to search common locations) 


## Usage

Once you have configured the path to `elm-test` (see above), right-click anywhere within an Elm file and select **Run (Tests in $YourProjectName)**.


## Demo

![elm-test GUI](../assets/elm_test_runner.jpg)
