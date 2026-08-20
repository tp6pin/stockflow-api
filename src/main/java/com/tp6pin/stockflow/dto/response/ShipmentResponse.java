package com.tp6pin.stockflow.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.tp6pin.stockflow.entity.Shipment;
import com.tp6pin.stockflow.enums.ShipmentStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShipmentResponse {

    private Long id;

    private String shipmentNumber;

    private Long orderId;

    private String orderNumber;

    private ShipmentStatus status;

    private String carrier;

    private String trackingNumber;

    private String recipientName;

    private String recipientPhone;

    private String shippingAddress;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    private String note;

    private Long createdById;

    private String createdByName;

    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ShipmentItemResponse> items;

    public static ShipmentResponse from(
            Shipment shipment
    ) {
        ShipmentResponse response =
            new ShipmentResponse();

        response.setId(shipment.getId());

        response.setShipmentNumber(
            shipment.getShipmentNumber()
        );

        response.setOrderId(
            shipment.getOrder().getId()
        );

        response.setOrderNumber(
            shipment.getOrder().getOrderNumber()
        );

        response.setStatus(
            shipment.getStatus()
        );

        response.setCarrier(
            shipment.getCarrier()
        );

        response.setTrackingNumber(
            shipment.getTrackingNumber()
        );

        response.setRecipientName(
            shipment.getRecipientName()
        );

        response.setRecipientPhone(
            shipment.getRecipientPhone()
        );

        response.setShippingAddress(
            shipment.getShippingAddress()
        );

        response.setShippedAt(
            shipment.getShippedAt()
        );

        response.setDeliveredAt(
            shipment.getDeliveredAt()
        );

        response.setNote(
            shipment.getNote()
        );

        response.setCreatedById(
            shipment.getCreatedBy().getId()
        );

        response.setCreatedByName(
            shipment.getCreatedBy().getName()
        );

        response.setVersion(
            shipment.getVersion()
        );

        response.setCreatedAt(
            shipment.getCreatedAt()
        );

        response.setUpdatedAt(
            shipment.getUpdatedAt()
        );

        response.setItems(
            shipment.getItems()
                .stream()
                .map(ShipmentItemResponse::from)
                .toList()
        );

        return response;
    }
}