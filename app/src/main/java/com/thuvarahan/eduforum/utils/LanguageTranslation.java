/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.thuvarahan.eduforum.utils;

import android.util.Log;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.langdetect.MLDetectedLang;
import com.huawei.hms.mlsdk.langdetect.MLLangDetectorFactory;
import com.huawei.hms.mlsdk.langdetect.cloud.MLRemoteLangDetector;
import com.huawei.hms.mlsdk.langdetect.cloud.MLRemoteLangDetectorSetting;
import com.huawei.hms.mlsdk.translate.MLTranslateLanguage;
import com.huawei.hms.mlsdk.translate.MLTranslatorFactory;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslateSetting;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslator;

import java.util.List;
import java.util.Set;

public class LanguageTranslation {
    /**
     * Translation on the cloud. If you want to use cloud remoteTranslator,
     * you need to apply for an agconnect-services.json file in the developer
     * alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
     * replacing the sample-agconnect-services.json in the project.
     */
    private static void remoteTranslator(String sourceText, IRemoteTranslationTask remoteTranslationTask) {
        MLRemoteTranslateSetting setting =
                new MLRemoteTranslateSetting.Factory().setTargetLangCode("en").create();
        MLRemoteTranslator remoteTranslator = MLTranslatorFactory.getInstance().getRemoteTranslator(setting);
        Task<String> task = remoteTranslator.asyncTranslate(sourceText);
        task.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String text) {
                remoteTranslationTask.onTranslated(text);
                stopRemoteTranslator(remoteTranslator);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
                stopRemoteTranslator(remoteTranslator);
            }
        });
    }

    public static void queryAllLanguages() {
        MLTranslateLanguage.getCloudAllLanguages().addOnSuccessListener(
                new OnSuccessListener<Set<String>>() {
                    @Override
                    public void onSuccess(Set<String> result) {
                        Log.d("LANG", result.toString());
                    }
                });
    }

    private static void stopRemoteTranslator(MLRemoteTranslator mlRemoteTranslator) {
        if (mlRemoteTranslator != null) {
            mlRemoteTranslator.stop();
        }
    }

    public static boolean textContainsOtherLang(String text) {
        return text.chars().anyMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.TAMIL);
    }

    private static void detectLanguage(final String sourceText, IRemoteLanguageDetectionTask remoteLanguageDetectionTask) {
        MLRemoteLangDetectorSetting setting = new MLRemoteLangDetectorSetting.Factory()
                .setTrustedThreshold(0.01f)
                .create();
        MLRemoteLangDetector mlRemoteLangDetector = MLLangDetectorFactory.getInstance()
                .getRemoteLangDetector(setting);
        Task<List<MLDetectedLang>> probabilityDetectTask = mlRemoteLangDetector.probabilityDetect(sourceText);
        probabilityDetectTask.addOnSuccessListener(new OnSuccessListener<List<MLDetectedLang>>() {//
            @Override
            public void onSuccess(List<MLDetectedLang> mlDetectedLangs) {
                remoteLanguageDetectionTask.onResult((mlDetectedLangs.size() <= 1) && (mlDetectedLangs.get(0).getLangCode().equals("en")));
                stopRemoteLanguageDetector(mlRemoteLangDetector);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
                stopRemoteLanguageDetector(mlRemoteLangDetector);
            }
        });
    }

    private static void stopRemoteLanguageDetector(MLRemoteLangDetector mlRemoteLangDetector) {
        if (mlRemoteLangDetector != null) {
            mlRemoteLangDetector.stop();
        }
    }

    public static void getTranslatedText(String originalText, ITranslationTask translationTask) {
        detectLanguage(originalText, new IRemoteLanguageDetectionTask() {
            @Override
            public void onResult(boolean isEnglish) {
                if (!isEnglish) {
                    remoteTranslator(originalText, new IRemoteTranslationTask() {
                        @Override
                        public void onTranslated(String text) {
                            if (!((text.trim()).equals(originalText.trim()))) {
                                translationTask.onResult(true, text);
                            } else {
                                translationTask.onResult(false, null);
                            }
                        }
                    });
                }
            }
        });
    }

    public interface IRemoteLanguageDetectionTask {
        void onResult(boolean isEnglish);
    }

    public interface IRemoteTranslationTask {
        void onTranslated(String text);
    }

    public interface ITranslationTask {
        void onResult(boolean isTranslated, String text);
    }
}
