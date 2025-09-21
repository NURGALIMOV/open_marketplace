package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplyOrderBundleItem {
    private long sku;
    private int quantity;
    @JsonProperty("offer_id")
    private String offerId;
    @JsonProperty("icon_path")
    private String iconPath;
    private String name;
    @JsonProperty("volume_in_litres")
    private double volumeInLitres;
    @JsonProperty("total_volume_in_litres")
    private double totalVolumeInLitres;
    private String barcode;
    @JsonProperty("product_id")
    private long productId;
    private int quant;
    @JsonProperty("sfbo_attribute")
    private String sfboAttribute;
    @JsonProperty("shipment_type")
    private String shipmentType;
    @JsonProperty("is_quant_editable")
    private boolean isQuantEditable;
    private List<String> tags;
    @JsonProperty("placement_zone")
    private String placementZone;
}
