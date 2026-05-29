
public interface OfferingRepository extends JpaRepository<Offering, UUID> {
 
 @Query("SELECT o FROM Offering o JOIN FETCH o.course JOIN FETCH o.teacher " +
           "WHERE o.status = 'ACTIVE' ORDER BY o.createdAt DESC")
    List<Offering> findAllActive();

}