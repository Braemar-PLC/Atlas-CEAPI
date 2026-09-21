namespace Atlas.Web.Ice.Domain.Services;

public interface IIceMessageProcessor
{
    void Process(string line);
}
