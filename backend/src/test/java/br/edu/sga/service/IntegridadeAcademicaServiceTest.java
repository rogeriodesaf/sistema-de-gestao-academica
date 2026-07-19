package br.edu.sga.service;

import br.edu.sga.entity.AnoLetivo;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.PeriodoLetivo;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegridadeAcademicaServiceTest {
    private final IntegridadeAcademicaService service = new IntegridadeAcademicaService();

    @Test
    void deveDetectarSobreposicaoDeDiaHorarioEIntervaloAcademico() {
        OfertaDisciplina primeira = oferta("Segunda-feira, 19h às 21h",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 5, 31));
        OfertaDisciplina segunda = oferta("Segunda 20:00-22:00",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));

        assertTrue(service.sobrepoe(primeira, segunda));
    }

    @Test
    void naoDeveBloquearDiasOuIntervalosDeDatasDiferentes() {
        OfertaDisciplina primeira = oferta("Segunda-feira, 19h às 21h",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 5, 31));
        OfertaDisciplina outroDia = oferta("Terça-feira, 19h às 21h",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 5, 31));
        OfertaDisciplina outrasDatas = oferta("Segunda-feira, 19h às 21h",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 30));

        assertFalse(service.sobrepoe(primeira, outroDia));
        assertFalse(service.sobrepoe(primeira, outrasDatas));
    }

    private OfertaDisciplina oferta(String horario, LocalDate inicio, LocalDate fim) {
        AnoLetivo ano = new AnoLetivo();
        ano.dataInicio = LocalDate.of(2026, 1, 1);
        ano.dataFim = LocalDate.of(2026, 12, 31);
        PeriodoLetivo periodo = new PeriodoLetivo();
        periodo.anoLetivo = ano;
        periodo.dataInicio = inicio;
        periodo.dataFim = fim;
        OfertaDisciplina oferta = new OfertaDisciplina();
        oferta.anoLetivo = ano;
        oferta.periodoLetivo = periodo;
        oferta.dataInicio = inicio;
        oferta.dataFim = fim;
        oferta.horario = horario;
        return oferta;
    }
}
