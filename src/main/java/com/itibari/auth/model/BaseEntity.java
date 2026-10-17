package com.itibari.auth.model;

import com.itibari.auth.util.date.FormatDate;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    @Column(name = "public_id", updatable = false)
    private UUID publicId;

    @Column(name = "creation_time", updatable = false)
    private LocalDateTime creationTime;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Column(name = "deleted")
    private boolean deleted;

    @PrePersist
    public void prePersist() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
        this.creationTime = FormatDate.currentNairobiDateTime();
        this.deleted = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.lastModifiedDate = FormatDate.currentNairobiDateTime();
    }
}
