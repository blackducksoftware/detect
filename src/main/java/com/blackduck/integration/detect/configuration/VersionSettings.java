package com.blackduck.integration.detect.configuration;

/**
 * POJO representing version settings from the project settings JSON.
 * 
 * Expected JSON structure:
 * {
 *   "versionName": "1.0.0",
 *   "nickname": "Release Candidate",
 *   "releaseComments" : "releaseComments",
 *   "phase": "RELEASED",
 *   "distribution": "EXTERNAL",
 *   "update": true
 * }
 */
public class VersionSettings {
    private String versionName;
    private String nickname;
    private String releaseComments;
    private String phase;
    private String distribution;
    private Boolean update;

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getReleaseComments() {
        return releaseComments;
    }

    public void setReleaseComments(String releaseComments) {
        this.releaseComments = releaseComments;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public Boolean getUpdate() {
        return update;
    }

    public void setUpdate(Boolean update) {
        this.update = update;
    }
}