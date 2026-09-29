namespace Atlas.Web.Core.Application;

/// <summary>A desk could not be saved as given; the message names what was wrong, in words meant for the person who typed it.</summary>
public sealed class DeskValidationException : Exception
{
    public DeskValidationException(string message) : base(message)
    {
    }
}
