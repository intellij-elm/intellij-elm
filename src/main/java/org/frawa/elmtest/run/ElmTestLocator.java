package org.frawa.elmtest.run;

import com.intellij.execution.testframework.sm.FileUrlProvider;

public class ElmTestLocator extends FileUrlProvider {
    public static final ElmTestLocator INSTANCE = new ElmTestLocator();

    private ElmTestLocator() {
    }
}
