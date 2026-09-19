package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pauta_documento",
       uniqueConstraints = @UniqueConstraint(name = "uq_pauta_documento", columnNames = {"id_asignatura", "tipo", "numero"}))
public class PautaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pauta_documento")
    private Long idPautaDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_asignatura", nullable = false)
    private Asignatura asignatura;

    @Column(name = "ponderacion", nullable = false, precision = 5, scale = 2)
    private BigDecimal ponderacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoDocumento tipo;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @OneToMany(mappedBy = "pautaDocumento")
    private List<DocumentoEstudiante> documentos = new ArrayList<>();

    public PautaDocumento() {
    }

    public PautaDocumento(Asignatura asignatura, BigDecimal ponderacion, TipoDocumento tipo, Integer numero) {
        this.asignatura = asignatura;
        this.ponderacion = ponderacion;
        this.tipo = tipo;
        this.numero = numero;
    }

    public Long getIdPautaDocumento() {
        return idPautaDocumento;
    }

    public void setIdPautaDocumento(Long idPautaDocumento) {
        this.idPautaDocumento = idPautaDocumento;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public void setAsignatura(Asignatura asignatura) {
        this.asignatura = asignatura;
    }

    public BigDecimal getPonderacion() {
        return ponderacion;
    }

    public void setPonderacion(BigDecimal ponderacion) {
        this.ponderacion = ponderacion;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public void setTipo(TipoDocumento tipo) {
        this.tipo = tipo;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public List<DocumentoEstudiante> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<DocumentoEstudiante> documentos) {
        this.documentos = documentos;
    }
}
