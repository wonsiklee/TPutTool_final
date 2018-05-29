package com.android.LGSetupWizard.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.LGSetupWizard.database.TestResultDBManager;

import java.util.Date;

public class TestResultDTO {
    public int mIndex = 0;
    public Date mTestedTime;
    public TestResultDBManager.TestCategory mTestedCategory;
    public float mTestResult;
    public String mTestDescription;

    public TestResultDTO() {
        this.mIndex = 0;
        this.mTestedCategory = TestResultDBManager.TestCategory.ALL_TYPE;
        this.mTestResult = 0.0f;
        this.mTestDescription = "";
    }

    public TestResultDTO(int index, @NonNull Date testedTime, @NonNull TestResultDBManager.TestCategory category, float testResult, @Nullable String testDescription) {
        this.mIndex = index;
        this.mTestedTime = testedTime;
        this.mTestedCategory = category;
        this.mTestResult = testResult;
        this.mTestDescription = (testDescription == null) ? "" : testDescription;
    }

    public TestResultDTO(int index, @NonNull Date testedTime, @NonNull String category, float testResult, @Nullable String testDescription) {
        this.mIndex = index;
        this.mTestedTime = testedTime;
        this.mTestedCategory = TestResultDBManager.TestCategory.valueOf(category);
        this.mTestResult = testResult;
        this.mTestDescription = (testDescription == null) ? "" : testDescription;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("idx = ").append(this.mIndex).append("\n")
        .append("Time : ").append(this.mTestedTime).append("\n")
        .append("Category : ").append(this.mTestedCategory).append("\n")
        .append("Result : "). append(this.mTestResult).append(" Mbps").append("\n")
        .append("Description : ").append(this.mTestDescription);

        return sb.toString();
    }
}
