package com.zhuoyu.delivery.core.user.dto;

import java.util.List;

public record MenuItemResponse(
    String key,
    String label,
    String path,
    String icon,
    List<MenuItemResponse> children
) {

    public MenuItemResponse(String key, String label, String path, String icon) {
        this(key, label, path, icon, List.of());
    }
}
