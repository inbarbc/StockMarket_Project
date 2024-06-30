package Dtos.Rules;

public class TimeRangeInDayRuleDto extends RuleDto {
    public int startHour, startMinutes;
    public int endHour, endMinutes;

    public TimeRangeInDayRuleDto(int startHour, int startMinutes, int endHour, int endMinutes) {
        this.startHour = startHour;
        this.startMinutes = startMinutes;
        this.endHour = endHour;
        this.endMinutes = endMinutes;
    }
}
