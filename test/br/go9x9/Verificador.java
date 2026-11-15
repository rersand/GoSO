package br.go9x9;

public final class Verificador {
    private static int total;
    private static int falhas;

    private Verificador() {
    }

    public static void verdadeiro(String descricao, boolean condicao) {
        total++;
        if (condicao) {
            System.out.println("  [ok]    " + descricao);
        } else {
            falhas++;
            System.out.println("  [FALHA] " + descricao);
        }
    }

    public static void igual(String descricao, Object esperado, Object obtido) {
        total++;
        boolean iguais = esperado == null ? obtido == null : esperado.equals(obtido);
        if (iguais) {
            System.out.println("  [ok]    " + descricao);
        } else {
            falhas++;
            System.out.println("  [FALHA] " + descricao + " -> esperado <" + esperado + "> mas foi <" + obtido + ">");
        }
    }

    public static void secao(String titulo) {
        System.out.println();
        System.out.println("== " + titulo);
    }

    public static int finalizar() {
        System.out.println();
        System.out.println(total + " verificações, " + falhas + " falha(s).");
        return falhas == 0 ? 0 : 1;
    }
}
