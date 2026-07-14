package br.edu.sga.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class HistoricoPdfService {
    private static final Charset PDF_CHARSET = Charset.forName("windows-1252");
    private static final int REGISTROS_POR_PAGINA = 24;
    private static final double[] COLUNAS = {95, 220, 35, 42, 58, 95};
    private static final String[] CABECALHOS = {"PERÍODO", "DISCIPLINA", "CH", "NOTA", "FREQUÊNCIA", "SITUAÇÃO"};

    public byte[] gerar(List<String> linhas) {
        Documento documento = separar(linhas);
        List<List<String[]>> paginas = paginar(documento.registros());
        byte[] logo = carregarLogo();

        List<byte[]> objetos = new ArrayList<>();
        objetos.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder filhos = new StringBuilder();
        for (int i = 0; i < paginas.size(); i++) filhos.append(6 + i * 2).append(" 0 R ");
        objetos.add(bytes("<< /Type /Pages /Kids [" + filhos + "] /Count " + paginas.size() + " >>"));
        objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        objetos.add(objetoImagem(logo));

        for (int i = 0; i < paginas.size(); i++) {
            int paginaObj = 6 + i * 2;
            int conteudoObj = paginaObj + 1;
            objetos.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> /XObject << /Logo 5 0 R >> >> "
                    + "/Contents " + conteudoObj + " 0 R >>"));
            String conteudo = conteudo(documento, paginas.get(i), i + 1, paginas.size());
            objetos.add(bytes("<< /Length " + bytes(conteudo).length + " >>\nstream\n" + conteudo + "\nendstream"));
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        escrever(pdf, "%PDF-1.4\n%âãÏÓ\n");
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(pdf.size());
            escrever(pdf, (i + 1) + " 0 obj\n");
            pdf.writeBytes(objetos.get(i));
            escrever(pdf, "\nendobj\n");
        }
        int xref = pdf.size();
        escrever(pdf, "xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
        for (int i = 1; i < offsets.size(); i++) escrever(pdf, String.format("%010d 00000 n \n", offsets.get(i)));
        escrever(pdf, "trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF");
        return pdf.toByteArray();
    }

    private String conteudo(Documento documento, List<String[]> registros, int pagina, int total) {
        StringBuilder pdf = new StringBuilder();
        pdf.append("q 70 0 0 70 45 744 cm /Logo Do Q\n");
        texto(pdf, "F2", 12, 130, 796, "SEMINÁRIO TEOLÓGICO CONGREGACIONAL DE JOÃO PESSOA");
        texto(pdf, "F2", 18, 130, 770, "HISTÓRICO ESCOLAR");
        pdf.append("0.07 0.22 0.34 RG 1.5 w 40 735 m 555 735 l S\n");

        for (int i = 0; i < documento.identificacao().size(); i++) {
            texto(pdf, i == 0 ? "F2" : "F1", 9, 45, 708 - i * 18, documento.identificacao().get(i));
        }

        double topo = 645;
        double xInicial = 25;
        pdf.append("0.06 0.18 0.29 rg ").append(xInicial).append(' ').append(topo - 20)
                .append(" 545 20 re f\n");
        double x = xInicial;
        for (int i = 0; i < CABECALHOS.length; i++) {
            textoBranco(pdf, "F2", 7, x + 4, topo - 14, CABECALHOS[i]);
            x += COLUNAS[i];
        }

        for (int linha = 0; linha < registros.size(); linha++) {
            double base = topo - 20 - (linha + 1) * 16;
            if (linha % 2 == 0) pdf.append("0.95 0.97 0.98 rg 25 ").append(base).append(" 545 16 re f\n");
            x = xInicial;
            String[] registro = registros.get(linha);
            for (int coluna = 0; coluna < COLUNAS.length; coluna++) {
                texto(pdf, coluna == 1 ? "F2" : "F1", 7, x + 4, base + 5,
                        limitar(registro[coluna], limite(coluna)));
                x += COLUNAS[coluna];
            }
        }

        double fundoTabela = topo - 20 - registros.size() * 16;
        pdf.append("0.72 0.78 0.82 RG .45 w 25 ").append(fundoTabela).append(" 545 ")
                .append(20 + registros.size() * 16).append(" re S\n");
        x = xInicial;
        for (double largura : COLUNAS) {
            pdf.append(x).append(' ').append(fundoTabela).append(" m ").append(x).append(' ')
                    .append(topo).append(" l S\n");
            x += largura;
        }
        pdf.append("570 ").append(fundoTabela).append(" m 570 ").append(topo).append(" l S\n");
        for (int i = 0; i <= registros.size(); i++) {
            double y = topo - 20 - i * 16;
            pdf.append("25 ").append(y).append(" m 570 ").append(y).append(" l S\n");
        }

        if (pagina == total && !documento.resumo().isEmpty()) {
            double yResumo = fundoTabela - 24;
            pdf.append("0.91 0.95 0.98 rg 25 ").append(yResumo - documento.resumo().size() * 13 - 10)
                    .append(" 545 ").append(documento.resumo().size() * 13 + 20).append(" re f\n");
            texto(pdf, "F2", 9, 35, yResumo, "RESUMO DA INTEGRALIZAÇÃO");
            for (int i = 0; i < documento.resumo().size(); i++) {
                texto(pdf, "F1", 8, 35, yResumo - 17 - i * 13, documento.resumo().get(i));
            }
        }

        pdf.append("0.55 0.62 0.67 RG .5 w 40 42 m 555 42 l S\n");
        texto(pdf, "F1", 7, 40, 28, "Documento acadêmico emitido pelo Sistema de Gestão Acadêmica");
        texto(pdf, "F1", 7, 500, 28, "Página " + pagina + " de " + total);
        return pdf.toString();
    }

    private Documento separar(List<String> linhas) {
        List<String> identificacao = new ArrayList<>();
        List<String[]> registros = new ArrayList<>();
        List<String> resumo = new ArrayList<>();
        boolean tabela = false;
        boolean depoisTabela = false;
        for (String linha : linhas) {
            if (linha == null || linha.isBlank()) {
                if (tabela) depoisTabela = true;
                continue;
            }
            if (linha.startsWith("PERIODO |") || linha.startsWith("PERÍODO |")) {
                tabela = true;
                continue;
            }
            if (!tabela) {
                if (!linha.startsWith("SGA -")) identificacao.add(linha);
            } else if (!depoisTabela && linha.contains("|")) {
                String[] colunas = linha.split("\\s*\\|\\s*", -1);
                registros.add(Arrays.copyOf(colunas, 6));
            } else {
                depoisTabela = true;
                resumo.add(linha);
            }
        }
        return new Documento(identificacao, registros, resumo);
    }

    private List<List<String[]>> paginar(List<String[]> registros) {
        List<List<String[]>> paginas = new ArrayList<>();
        for (int inicio = 0; inicio < registros.size(); inicio += REGISTROS_POR_PAGINA) {
            paginas.add(registros.subList(inicio, Math.min(inicio + REGISTROS_POR_PAGINA, registros.size())));
        }
        if (paginas.isEmpty()) paginas.add(List.of());
        return paginas;
    }

    private byte[] carregarLogo() {
        try (InputStream entrada = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("logo-seminario.jpg")) {
            if (entrada == null) throw new IllegalStateException("Logo institucional não encontrada");
            return entrada.readAllBytes();
        } catch (IOException erro) {
            throw new IllegalStateException("Não foi possível carregar a logo institucional", erro);
        }
    }

    private byte[] objetoImagem(byte[] logo) {
        ByteArrayOutputStream objeto = new ByteArrayOutputStream();
        escrever(objeto, "<< /Type /XObject /Subtype /Image /Width 500 /Height 500 /ColorSpace /DeviceRGB "
                + "/BitsPerComponent 8 /Filter /DCTDecode /Length " + logo.length + " >>\nstream\n");
        objeto.writeBytes(logo);
        escrever(objeto, "\nendstream");
        return objeto.toByteArray();
    }

    private void texto(StringBuilder pdf, String fonte, double tamanho, double x, double y, String valor) {
        pdf.append("BT /").append(fonte).append(' ').append(tamanho).append(" Tf 0 g ")
                .append(x).append(' ').append(y).append(" Td (").append(escapar(valor)).append(") Tj ET\n");
    }

    private void textoBranco(StringBuilder pdf, String fonte, double tamanho, double x, double y, String valor) {
        pdf.append("BT /").append(fonte).append(' ').append(tamanho).append(" Tf 1 g ")
                .append(x).append(' ').append(y).append(" Td (").append(escapar(valor)).append(") Tj ET\n");
    }

    private int limite(int coluna) {
        return switch (coluna) {
            case 0 -> 21;
            case 1 -> 48;
            case 4 -> 12;
            case 5 -> 20;
            default -> 8;
        };
    }

    private String limitar(String valor, int limite) {
        if (valor == null) return "-";
        return valor.length() <= limite ? valor : valor.substring(0, Math.max(1, limite - 3)) + "...";
    }

    private String escapar(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private byte[] bytes(String valor) { return valor.getBytes(PDF_CHARSET); }
    private void escrever(ByteArrayOutputStream saida, String valor) { saida.writeBytes(bytes(valor)); }

    private record Documento(List<String> identificacao, List<String[]> registros, List<String> resumo) {}
}
