class OfferingService{

    private final OfferingRepository offeringRepository;
     @Transactional(readOnly = true)
    public List<OfferingResponse> getAvailableOfferings(String viewerTimezone) {
        return offeringRepository.findAllActive()
                .stream()
                .map(o -> toOfferingResponse(o, viewerTimezone))
                .collect(Collectors.toList());
    }
}