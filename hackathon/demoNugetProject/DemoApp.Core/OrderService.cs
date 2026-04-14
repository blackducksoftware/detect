using Newtonsoft.Json;
using Serilog;

namespace DemoApp.Core;

/// <summary>
/// Core business service used to demonstrate a production component
/// with runtime dependencies (Newtonsoft.Json, Serilog) and
/// dev-only analyzers (Microsoft.CodeAnalysis.NetAnalyzers).
/// </summary>
public class OrderService
{
    private readonly ILogger _logger;

    public OrderService()
    {
        _logger = new LoggerConfiguration()
            .WriteTo.Console()
            .CreateLogger();
    }

    public string ProcessOrder(string customerId, decimal amount)
    {
        var order = new
        {
            CustomerId = customerId,
            Amount = amount,
            Timestamp = DateTime.UtcNow,
            Status = "Processed"
        };

        string json = JsonConvert.SerializeObject(order, Formatting.Indented);
        _logger.Information("Order processed: {OrderJson}", json);
        return json;
    }
}

