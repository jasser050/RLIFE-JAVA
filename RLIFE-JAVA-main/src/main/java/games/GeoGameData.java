package games;

import java.util.List;

public final class GeoGameData {
    private GeoGameData() {}

    public record FlagCountry(String flag, String country) {}
    public record CapitalEntry(String country, String flag, String capital) {}
    public record MapRegion(String country, String continent, double x, double y, double w, double h) {}

    public static final List<FlagCountry> FLAGS = List.of(
            new FlagCountry("🇫🇷", "France"), new FlagCountry("🇩🇪", "Germany"), new FlagCountry("🇬🇧", "United Kingdom"),
            new FlagCountry("🇺🇸", "USA"), new FlagCountry("🇯🇵", "Japan"), new FlagCountry("🇨🇳", "China"),
            new FlagCountry("🇧🇷", "Brazil"), new FlagCountry("🇮🇳", "India"), new FlagCountry("🇮🇹", "Italy"),
            new FlagCountry("🇪🇸", "Spain"), new FlagCountry("🇵🇹", "Portugal"), new FlagCountry("🇳🇱", "Netherlands"),
            new FlagCountry("🇧🇪", "Belgium"), new FlagCountry("🇨🇭", "Switzerland"), new FlagCountry("🇦🇹", "Austria"),
            new FlagCountry("🇸🇪", "Sweden"), new FlagCountry("🇳🇴", "Norway"), new FlagCountry("🇩🇰", "Denmark"),
            new FlagCountry("🇫🇮", "Finland"), new FlagCountry("🇵🇱", "Poland"), new FlagCountry("🇷🇺", "Russia"),
            new FlagCountry("🇺🇦", "Ukraine"), new FlagCountry("🇹🇷", "Turkey"), new FlagCountry("🇸🇦", "Saudi Arabia"),
            new FlagCountry("🇦🇪", "UAE"), new FlagCountry("🇪🇬", "Egypt"), new FlagCountry("🇲🇦", "Morocco"),
            new FlagCountry("🇹🇳", "Tunisia"), new FlagCountry("🇿🇦", "South Africa"), new FlagCountry("🇰🇪", "Kenya"),
            new FlagCountry("🇳🇬", "Nigeria"), new FlagCountry("🇬🇭", "Ghana"), new FlagCountry("🇦🇺", "Australia"),
            new FlagCountry("🇳🇿", "New Zealand"), new FlagCountry("🇨🇦", "Canada"), new FlagCountry("🇲🇽", "Mexico"),
            new FlagCountry("🇦🇷", "Argentina"), new FlagCountry("🇨🇱", "Chile"), new FlagCountry("🇨🇴", "Colombia"),
            new FlagCountry("🇵🇪", "Peru"), new FlagCountry("🇰🇷", "South Korea"), new FlagCountry("🇹🇭", "Thailand"),
            new FlagCountry("🇻🇳", "Vietnam"), new FlagCountry("🇮🇩", "Indonesia"), new FlagCountry("🇲🇾", "Malaysia"),
            new FlagCountry("🇵🇭", "Philippines"), new FlagCountry("🇵🇰", "Pakistan"), new FlagCountry("🇮🇷", "Iran"),
            new FlagCountry("🇮🇶", "Iraq"), new FlagCountry("🇯🇴", "Jordan")
    );

    public static final List<CapitalEntry> CAPITALS = List.of(
            new CapitalEntry("France", "🇫🇷", "Paris"), new CapitalEntry("Germany", "🇩🇪", "Berlin"),
            new CapitalEntry("United Kingdom", "🇬🇧", "London"), new CapitalEntry("USA", "🇺🇸", "Washington D.C."),
            new CapitalEntry("Japan", "🇯🇵", "Tokyo"), new CapitalEntry("China", "🇨🇳", "Beijing"),
            new CapitalEntry("Brazil", "🇧🇷", "Brasilia"), new CapitalEntry("India", "🇮🇳", "New Delhi"),
            new CapitalEntry("Italy", "🇮🇹", "Rome"), new CapitalEntry("Spain", "🇪🇸", "Madrid"),
            new CapitalEntry("Portugal", "🇵🇹", "Lisbon"), new CapitalEntry("Australia", "🇦🇺", "Canberra"),
            new CapitalEntry("Canada", "🇨🇦", "Ottawa"), new CapitalEntry("Russia", "🇷🇺", "Moscow"),
            new CapitalEntry("Egypt", "🇪🇬", "Cairo"), new CapitalEntry("Morocco", "🇲🇦", "Rabat"),
            new CapitalEntry("Tunisia", "🇹🇳", "Tunis"), new CapitalEntry("South Africa", "🇿🇦", "Pretoria"),
            new CapitalEntry("Kenya", "🇰🇪", "Nairobi"), new CapitalEntry("Saudi Arabia", "🇸🇦", "Riyadh"),
            new CapitalEntry("UAE", "🇦🇪", "Abu Dhabi"), new CapitalEntry("Turkey", "🇹🇷", "Ankara"),
            new CapitalEntry("South Korea", "🇰🇷", "Seoul"), new CapitalEntry("Thailand", "🇹🇭", "Bangkok"),
            new CapitalEntry("Argentina", "🇦🇷", "Buenos Aires"), new CapitalEntry("Mexico", "🇲🇽", "Mexico City"),
            new CapitalEntry("Chile", "🇨🇱", "Santiago"), new CapitalEntry("Colombia", "🇨🇴", "Bogota"),
            new CapitalEntry("Peru", "🇵🇪", "Lima"), new CapitalEntry("Pakistan", "🇵🇰", "Islamabad"),
            new CapitalEntry("Iran", "🇮🇷", "Tehran"), new CapitalEntry("Iraq", "🇮🇶", "Baghdad"),
            new CapitalEntry("Jordan", "🇯🇴", "Amman"), new CapitalEntry("Switzerland", "🇨🇭", "Bern"),
            new CapitalEntry("Austria", "🇦🇹", "Vienna"), new CapitalEntry("Belgium", "🇧🇪", "Brussels"),
            new CapitalEntry("Netherlands", "🇳🇱", "Amsterdam"), new CapitalEntry("Sweden", "🇸🇪", "Stockholm"),
            new CapitalEntry("Norway", "🇳🇴", "Oslo"), new CapitalEntry("Denmark", "🇩🇰", "Copenhagen"),
            new CapitalEntry("Finland", "🇫🇮", "Helsinki"), new CapitalEntry("Poland", "🇵🇱", "Warsaw"),
            new CapitalEntry("Ukraine", "🇺🇦", "Kyiv"), new CapitalEntry("Nigeria", "🇳🇬", "Abuja"),
            new CapitalEntry("Ghana", "🇬🇭", "Accra"), new CapitalEntry("New Zealand", "🇳🇿", "Wellington"),
            new CapitalEntry("Vietnam", "🇻🇳", "Hanoi"), new CapitalEntry("Indonesia", "🇮🇩", "Jakarta"),
            new CapitalEntry("Malaysia", "🇲🇾", "Kuala Lumpur"), new CapitalEntry("Philippines", "🇵🇭", "Manila")
    );

    public static final List<MapRegion> MAP_REGIONS = List.of(
            new MapRegion("France", "Europe", 340, 170, 28, 20),
            new MapRegion("Germany", "Europe", 372, 162, 28, 20),
            new MapRegion("Spain", "Europe", 320, 198, 34, 20),
            new MapRegion("Italy", "Europe", 383, 194, 24, 24),
            new MapRegion("UK", "Europe", 312, 146, 22, 20),
            new MapRegion("USA", "Americas", 130, 170, 70, 35),
            new MapRegion("Canada", "Americas", 120, 132, 90, 30),
            new MapRegion("Mexico", "Americas", 172, 210, 40, 24),
            new MapRegion("Brazil", "Americas", 222, 286, 60, 50),
            new MapRegion("Argentina", "Americas", 236, 350, 35, 60),
            new MapRegion("Egypt", "Africa", 410, 248, 25, 20),
            new MapRegion("Morocco", "Africa", 360, 248, 25, 20),
            new MapRegion("Tunisia", "Africa", 392, 250, 16, 18),
            new MapRegion("South Africa", "Africa", 426, 375, 38, 24),
            new MapRegion("Saudi Arabia", "Asia", 456, 246, 42, 26),
            new MapRegion("India", "Asia", 530, 264, 36, 30),
            new MapRegion("China", "Asia", 570, 216, 70, 48),
            new MapRegion("Japan", "Asia", 658, 226, 20, 28),
            new MapRegion("South Korea", "Asia", 634, 226, 18, 20),
            new MapRegion("Australia", "Oceania", 642, 358, 74, 44)
    );
}
