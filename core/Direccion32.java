package core;

import java.util.Objects;

/**
 * Representa una dirección física o lógica de 32 bits (0x00000000 a 0xFFFFFFFF).
 * Esta clase es inmutable y aplica encapsulamiento estricto.
 */
public final class Direccion32 implements Comparable<Direccion32> {
    public static final long MIN_VAL = 0x00000000L;
    public static final long MAX_VAL = 0xFFFFFFFFL;

    public static final Direccion32 CERO = new Direccion32(0);

    private final long valor;

    public Direccion32(long valor) {
        if (valor < MIN_VAL || valor > MAX_VAL) {
            throw new IllegalArgumentException(
                String.format("La dirección debe estar entre 0x%08X y 0x%08X. Valor provisto: 0x%X", MIN_VAL, MAX_VAL, valor)
            );
        }
        this.valor = valor;
    }

    public long getValor() {
        return valor;
    }

    public Direccion32 sumar(long offset) {
        return new Direccion32((this.valor + offset) & MAX_VAL);
    }

    public Direccion32 incrementar() {
        return sumar(1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Direccion32 that = (Direccion32) o;
        return valor == that.valor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return String.format("0x%08X", valor);
    }

    @Override
    public int compareTo(Direccion32 o) {
        return Long.compare(this.valor, o.valor);
    }
}
