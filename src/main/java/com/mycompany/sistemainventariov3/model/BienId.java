package com.mycompany.sistemainventariov3.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave compuesta para todas las entidades de bienes/equipos
 * Utiliza: codigo_sbai_original + item
 */
public class BienId implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String codigoSbaiOriginal;
    private Integer item;

    public BienId() {
    }

    public BienId(String codigoSbaiOriginal, Integer item) {
        this.codigoSbaiOriginal = codigoSbaiOriginal;
        this.item = item;
    }

    public String getCodigoSbaiOriginal() {
        return codigoSbaiOriginal;
    }

    public void setCodigoSbaiOriginal(String codigoSbaiOriginal) {
        this.codigoSbaiOriginal = codigoSbaiOriginal;
    }

    public Integer getItem() {
        return item;
    }

    public void setItem(Integer item) {
        this.item = item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BienId bienId = (BienId) o;
        return Objects.equals(codigoSbaiOriginal, bienId.codigoSbaiOriginal) &&
               Objects.equals(item, bienId.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoSbaiOriginal, item);
    }

    @Override
    public String toString() {
        return "BienId{" +
                "codigoSbaiOriginal='" + codigoSbaiOriginal + '\'' +
                ", item=" + item +
                '}';
    }
}
