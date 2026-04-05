package com.dirmove;

public class DirectoryModel {
    private String name;
    private String path;
    private boolean isSelected;

    public DirectoryModel(String name, String path, boolean isSelected) {
        this.name = name;
        this.path = path;
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
