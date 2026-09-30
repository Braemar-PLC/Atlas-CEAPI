using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Ports;

/// <summary>The calculator's matrix for a product, as it stands today: every expiry the rules pick and every listed strike.</summary>
public interface IOptionMatrixProvider
{
    /// <summary>The matrix for "TTF", "EUA", "WTI" or "Brent" (any case), or null for a product without option rules.</summary>
    OptionMatrix? Find(string product);
}
