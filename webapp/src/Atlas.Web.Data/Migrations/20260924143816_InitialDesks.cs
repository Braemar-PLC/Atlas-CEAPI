using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Atlas.Web.Data.Migrations
{
    /// <inheritdoc />
    public partial class InitialDesks : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "Desks",
                columns: table => new
                {
                    Key = table.Column<string>(type: "TEXT", nullable: false),
                    Name = table.Column<string>(type: "TEXT", nullable: false),
                    Description = table.Column<string>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Desks", x => x.Key);
                });

            migrationBuilder.CreateTable(
                name: "DeskMembers",
                columns: table => new
                {
                    DeskKey = table.Column<string>(type: "TEXT", nullable: false),
                    Email = table.Column<string>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_DeskMembers", x => new { x.DeskKey, x.Email });
                    table.ForeignKey(
                        name: "FK_DeskMembers_Desks_DeskKey",
                        column: x => x.DeskKey,
                        principalTable: "Desks",
                        principalColumn: "Key",
                        onDelete: ReferentialAction.Cascade);
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "DeskMembers");

            migrationBuilder.DropTable(
                name: "Desks");
        }
    }
}
