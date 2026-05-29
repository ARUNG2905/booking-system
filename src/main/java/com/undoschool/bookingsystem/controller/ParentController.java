@RestController
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
class ParentController{

    private final OfferingService offeringService;
@GetMapping("/offerings")
public ResponseEntity<List<OfferingResponse>> getAvailableOfferings(
            @AuthenticationPrincipal User parent) {
        return ResponseEntity.ok(offeringService.getAvailableOfferings(parent.getTimezone()));
    }
}