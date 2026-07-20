package br.edu.sga.service;

import br.edu.sga.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class ArquivoPdfService {
    private static final Logger LOG = Logger.getLogger(ArquivoPdfService.class);

    @ConfigProperty(name = "sga.upload.dir")
    String diretorioUpload;

    @ConfigProperty(name = "sga.upload.max-bytes")
    long tamanhoMaximo;

    public ArquivoSalvo salvarPdf(FileUpload arquivo, String pasta) {
        if (arquivo == null || arquivo.uploadedFile() == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Selecione um arquivo PDF");
        }

        String nomeOriginal = arquivo.fileName() == null ? "arquivo.pdf" : arquivo.fileName();
        String nomeNormalizado = nomeOriginal.toLowerCase(Locale.ROOT);
        String tipo = arquivo.contentType();

        try {
            long tamanho = Files.size(arquivo.uploadedFile());
            if (tamanho <= 0) {
                throw new ApiException(Response.Status.BAD_REQUEST, "O arquivo enviado esta vazio");
            }
            if (tamanho > tamanhoMaximo) {
                throw new ApiException(Response.Status.BAD_REQUEST, "O PDF deve ter no maximo " + (tamanhoMaximo / 1024 / 1024) + " MB");
            }
            if (!nomeNormalizado.endsWith(".pdf") || (tipo != null && !tipo.equalsIgnoreCase("application/pdf"))) {
                throw new ApiException(Response.Status.BAD_REQUEST, "Envie um arquivo no formato PDF");
            }
            try (InputStream entrada = Files.newInputStream(arquivo.uploadedFile())) {
                String inicio = new String(entrada.readNBytes(1024), StandardCharsets.ISO_8859_1);
                if (!inicio.contains("%PDF-")) {
                    throw new ApiException(Response.Status.BAD_REQUEST, "Envie um arquivo no formato PDF");
                }
            }

            Path destinoDir = Path.of(diretorioUpload, pasta).toAbsolutePath().normalize();
            Files.createDirectories(destinoDir);
            Path destino = destinoDir.resolve(UUID.randomUUID() + ".pdf");
            Files.copy(arquivo.uploadedFile(), destino, StandardCopyOption.REPLACE_EXISTING);
            return new ArquivoSalvo(destino.toString(), nomeOriginal, tipo == null ? "application/pdf" : tipo, tamanho);
        } catch (IOException e) {
            LOG.errorf(e, "Falha ao armazenar PDF '%s' em '%s'", nomeOriginal, pasta);
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR, "Nao foi possivel salvar o PDF");
        }
    }

    public void remover(String caminho) {
        if (caminho == null || caminho.isBlank()) return;
        try {
            Files.deleteIfExists(Path.of(caminho));
        } catch (IOException ignored) {
            // A remocao do registro nao deve falhar por um arquivo ausente ou bloqueado.
        }
    }

    public record ArquivoSalvo(String caminho, String nome, String tipo, long tamanho) {}
}
