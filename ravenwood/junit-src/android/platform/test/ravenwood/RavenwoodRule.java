/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.platform.test.ravenwood;

import static com.android.ravenwood.common.RavenwoodCommonUtils.log;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Instrumentation;
import android.content.Context;
import android.platform.test.annotations.DisabledOnRavenwood;

import androidx.test.platform.app.InstrumentationRegistry;

import com.android.ravenwood.common.RavenwoodCommonUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @deprecated This class is undergoing a major change. Reach out to g/ravenwood if you need
 * any featues in it.
 */
@Deprecated
public final class RavenwoodRule implements TestRule {
    private static final String TAG = "RavenwoodRule";

    static final boolean IS_ON_RAVENWOOD = RavenwoodCommonUtils.isOnRavenwood();

    /**
     * When this flag is enabled, all tests will be unconditionally run on Ravenwood to detect
     * cases where a test is able to pass despite being marked as {@link DisabledOnRavenwood}.
     *
     * This is typically helpful for internal maintainers discovering tests that had previously
     * been ignored, but now have enough Ravenwood-supported functionality to be enabled.
     */
    private static final boolean RUN_DISABLED_TESTS = "1".equals(
            System.getenv("RAVENWOOD_RUN_DISABLED_TESTS"));

    /**
     * When using ENABLE_PROBE_IGNORED, you may still want to skip certain tests,
     * for example because the test would crash the JVM.
     *
     * This regex defines the tests that should still be disabled even if ENABLE_PROBE_IGNORED
     * is set.
     *
     * Before running each test class and method, we check if this pattern can be found in
     * the full test name (either [class full name], or [class full name] + "#" + [method name]),
     * and if so, we skip it.
     *
     * For example, if you want to skip an entire test class, use:
     * RAVENWOOD_REALLY_DISABLE='\.CustomTileDefaultsRepositoryTest$'
     *
     * For example, if you want to skip an entire test class, use:
     * RAVENWOOD_REALLY_DISABLE='\.CustomTileDefaultsRepositoryTest#testSimple$'
     *
     * To ignore multiple classes, use (...|...), for example:
     * RAVENWOOD_REALLY_DISABLE='\.(ClassA|ClassB)$'
     *
     * Because we use a regex-find, setting "." would disable all tests.
     */
    private static final Pattern REALLY_DISABLED_PATTERN = Pattern.compile(
            Objects.requireNonNullElse(System.getenv("RAVENWOOD_REALLY_DISABLED"), ""));

    private static final boolean HAS_REALLY_DISABLE_PATTERN =
            !REALLY_DISABLED_PATTERN.pattern().isEmpty();

    static {
        if (RUN_DISABLED_TESTS) {
            log(TAG, "$RAVENWOOD_RUN_DISABLED_TESTS enabled: force running all tests");
            if (HAS_REALLY_DISABLE_PATTERN) {
                log(TAG, "$RAVENWOOD_REALLY_DISABLED=" + REALLY_DISABLED_PATTERN.pattern());
            }
        }
    }

    private final RavenwoodConfig mConfiguration;

    public RavenwoodRule() {
        mConfiguration = new RavenwoodConfig.Builder().build();
    }

    private RavenwoodRule(RavenwoodConfig config) {
        mConfiguration = config;
    }

    public static class Builder {
        private final RavenwoodConfig.Builder mBuilder =
                new RavenwoodConfig.Builder();

        public Builder() {
        }

        /**
         * @deprecated no longer used. We always use an app UID.
         */
        @Deprecated
        public Builder setProcessSystem() {
            return this;
        }

        /**
         * @deprecated no longer used. We always use an app UID.
         */
        @Deprecated
        public Builder setProcessApp() {
            return this;
        }

        /**
         * @deprecated no longer used.
         */
        @Deprecated
        public Builder setPackageName(@NonNull String packageName) {
            return this;
        }

        /**
         * @deprecated no longer used. Main thread is always available.
         */
        @Deprecated
        public Builder setProvideMainThread(boolean provideMainThread) {
            return this;
        }

        /**
         * Configure the given system property as immutable for the duration of the test.
         * Read access to the key is allowed, and write access will fail. When {@code value} is
         * {@code null}, the value is left as undefined.
         *
         * All properties in the {@code debug.*} namespace are automatically mutable, with no
         * developer action required.
         *
         * Has no effect on non-Ravenwood environments.
         */
        public Builder setSystemPropertyImmutable(@NonNull String key, @Nullable Object value) {
            mBuilder.setSystemPropertyImmutableReal(key, value);
            return this;
        }

        /**
         * Configure the given system property as mutable for the duration of the test.
         * Both read and write access to the key is allowed, and its value will be reset between
         * each test. When {@code value} is {@code null}, the value is left as undefined.
         *
         * All properties in the {@code debug.*} namespace are automatically mutable, with no
         * developer action required.
         *
         * Has no effect on non-Ravenwood environments.
         */
        public Builder setSystemPropertyMutable(@NonNull String key, @Nullable Object value) {
            mBuilder.setSystemPropertyMutableReal(key, value);
            return this;
        }

        /**
         * Configure the set of system services that are required for this test to operate.
         *
         * For example, passing {@code android.hardware.SerialManager.class} as an argument will
         * ensure that the underlying service is created, initialized, and ready to use for the
         * duration of the test. The {@code SerialManager} instance can be obtained via
         * {@code RavenwoodRule.getContext()} and {@code Context.getSystemService()}, and
         * {@code SerialManagerInternal} can be obtained via {@code LocalServices.getService()}.
         */
        public Builder setServicesRequired(@NonNull Class<?>... services) {
            mBuilder.setServicesRequired(services);
            return this;
        }

        public RavenwoodRule build() {
            return new RavenwoodRule(mBuilder.build());
        }
    }

    /**
     * @deprecated replaced by {@link #isOnRavenwood()}
     */
    @Deprecated
    public static boolean isUnderRavenwood() {
        return IS_ON_RAVENWOOD;
    }

    /**
     * Return if the current process is running on a Ravenwood test environment.
     */
    public static boolean isOnRavenwood() {
        return IS_ON_RAVENWOOD;
    }

    /**
     * @deprecated Use
     * {@code androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext()}
     * instead.
     */
    @Deprecated
    public Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getContext();
    }

    /**
     * @deprecated Use
     * {@code androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()}
     * instead.
     */
    @Deprecated
    public Instrumentation getInstrumentation() {
        return InstrumentationRegistry.getInstrumentation();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (!RavenwoodConfig.isOnRavenwood()) {
            return base;
        }
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                RavenwoodAwareTestRunner.onRavenwoodRuleEnter(description, RavenwoodRule.this);
                try {
                    base.evaluate();
                } finally {
                    RavenwoodAwareTestRunner.onRavenwoodRuleExit(description, RavenwoodRule.this);
                }
            }
        };
    }

    /**
     * Returns the "real" result from {@link System#currentTimeMillis()}.
     *
     * Currently, it's the same thing as calling {@link System#currentTimeMillis()},
     * but this one is guaranteeed to return the real value, even when Ravenwood supports
     * injecting a time to{@link System#currentTimeMillis()}.
     */
    public long realCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    // Below are internal to ravenwood. Don't use them from normal tests...

    public static class RavenwoodPrivate {
        private RavenwoodPrivate() {
        }

        private volatile Boolean mRunDisabledTestsOverride = null;

        private volatile Pattern mReallyDisabledPattern = null;

        public boolean isRunningDisabledTests() {
            if (mRunDisabledTestsOverride != null) {
                return mRunDisabledTestsOverride;
            }
            return RUN_DISABLED_TESTS;
        }

        public Pattern getReallyDisabledPattern() {
            if (mReallyDisabledPattern != null) {
                return mReallyDisabledPattern;
            }
            return REALLY_DISABLED_PATTERN;
        }

        public void overrideRunDisabledTest(boolean runDisabledTests,
                @Nullable String reallyDisabledPattern) {
            mRunDisabledTestsOverride = runDisabledTests;
            mReallyDisabledPattern =
                    reallyDisabledPattern == null ? null : Pattern.compile(reallyDisabledPattern);
        }

        public void resetRunDisabledTest() {
            mRunDisabledTestsOverride = null;
            mReallyDisabledPattern = null;
        }
    }

    private static final RavenwoodPrivate sRavenwoodPrivate = new  RavenwoodPrivate();

    public static RavenwoodPrivate private$ravenwood() {
        return sRavenwoodPrivate;
    }

    RavenwoodConfig getConfiguration() {
        return mConfiguration;
    }
}
