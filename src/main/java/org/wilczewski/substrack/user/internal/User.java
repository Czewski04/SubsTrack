package org.wilczewski.substrack.user.internal;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email",  nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserEmail> additionalEmails = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addAdditionalEmail(String email) {
        UserEmail newEmail = new UserEmail(email, this);
        this.additionalEmails.add(newEmail);
    }

    public void updateAdditionalEmail(UUID emailId, String newEmail) {
        for (UserEmail userEmail : additionalEmails) {
            if (userEmail.getId().equals(emailId)) {
                if(userEmail.getEmail().equals(this.email)) {
                    this.email = newEmail;
                }
                userEmail.setEmail(newEmail);
                return;
            }
        }
    }

    public void  deleteAdditionalEmail(UUID emailId) throws Exception {
        for (UserEmail userEmail : additionalEmails) {
            if (userEmail.getId().equals(emailId)) {
                if(userEmail.getEmail().equals(this.email)) {
                    throw new Exception("Cannot delete primary email");
                }
                additionalEmails.remove(userEmail);
            }
        }
    }
}
