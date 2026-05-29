@Entity
@Table(name = "users")
@Getter@Setter@AllArgsConstructor@NoArgsConstructor@Builder
class User{
@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, length = 100)
    private String timezone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

     @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    
}