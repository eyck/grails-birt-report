public interface BirtReportServiceInterface {
    // where are the reports located
    def getReportHome()
    // where is the BIRT platform installed
    def getEngineHome()
    // at which URL are the reports generated
    def getBaseURL()
    // at which URL are the images accessible
    def getBaseImageURL()
    // what is the image dir on disc
    def getImageDir()
}