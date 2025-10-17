CREATE FUNCTION GenerateStaffID()
RETURNS NVARCHAR(50)
AS
BEGIN
    DECLARE @NewID NVARCHAR(50);
    DECLARE @CurrentYear NVARCHAR(4) = CAST(YEAR(GETDATE()) AS NVARCHAR(4));
    DECLARE @Prefix NVARCHAR(10) = 'STF' + @CurrentYear + '-';

    DECLARE @MaxID NVARCHAR(50);
    SELECT @MaxID = MAX(id) FROM Staff WHERE id LIKE @Prefix + '%';

    IF @MaxID IS NULL
    BEGIN
        SET @NewID = @Prefix + '0001';
    END
    ELSE
    BEGIN
        DECLARE @NumericPart INT = CAST(SUBSTRING(@MaxID, LEN(@Prefix) + 1, LEN(@MaxID)) AS INT);
        SET @NumericPart = @NumericPart + 1;
        SET @NewID = @Prefix + RIGHT('0000' + CAST(@NumericPart AS NVARCHAR(4)), 4);
    END

    RETURN @NewID;
END;



