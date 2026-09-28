

package com.bitselect.agent.core.chunk.model;

import com.bitselect.agent.core.parser.model.AssetRef;
import com.bitselect.agent.core.parser.model.Provenance;
import lombok.Builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 块的结构化元数据
 */
@Builder
public record ChunkMetadata(
        List<String> outlinePath,
        List<AssetRef> assets,
        Provenance provenance,
        Map<String, Object> extras
) {

    public static final String KEY_ASSETS = "assets";
    public static final String KEY_SOURCE_FILE = "source_file";
    public static final String KEY_SHEET_NAME = "sheet_name";

    public ChunkMetadata {
        outlinePath = immutableCopy(outlinePath);
        assets = immutableCopy(assets);
        extras = extras == null || extras.isEmpty() ? Map.of() : Map.copyOf(extras);
    }

    public static ChunkMetadata empty() {
        return new ChunkMetadata(List.of(), List.of(), null, Map.of());
    }

    public ChunkMetadata withExtras(Map<String, Object> additional) {
        if (additional == null || additional.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new LinkedHashMap<>(extras);
        merged.putAll(additional);
        return new ChunkMetadata(outlinePath, assets, provenance, merged);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(extras);
        if (!assets.isEmpty()) {
            List<Map<String, Object>> assetMaps = new ArrayList<>(assets.size());
            for (AssetRef asset : assets) {
                Map<String, Object> one = new LinkedHashMap<>();
                putIfPresent(one, "url", asset.publicUrl());
                putIfPresent(one, "mime", asset.mime());
                assetMaps.add(one);
            }
            map.put(KEY_ASSETS, assetMaps);
        }
        if (provenance != null) {
            putIfPresent(map, KEY_SOURCE_FILE, provenance.sourceFile());
            putIfPresent(map, KEY_SHEET_NAME, provenance.sheetName());
        }
        return map;
    }

    private static void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private static <T> List<T> immutableCopy(List<T> source) {
        return source == null || source.isEmpty() ? List.of() : List.copyOf(source);
    }
}