package com.blackduck.integration.detect.configuration;

import com.google.gson.annotations.SerializedName;

/**
 * POJO representing version settings from the project settings JSON.
 * 
 * Expected JSON structure:
 * {
 *   "versionName": "1.0.0",
 *   "nickname": "Release Candidate",
 *   "notes": "Initial release candidate version",
 *   "phase": "RELEASED",
 *   "distribution": "EXTERNAL",
 *   "update": true,
 *   "license": "Apache-2.0"
 * }
 */
public class VersionSettings {
    @SerializedName("versionName")
    private String name;
    private String nickname;
    private String notes;
    private String phase;
    private String distribution;
    private Boolean update;
    private String license;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }
}