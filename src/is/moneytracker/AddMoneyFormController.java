/**
 *
 */
package is.moneytracker;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.type.DateType;

import is.moneytracker.model.Category;
import is.moneytracker.model.Transaction;
import is.moneytracker.model.TransactionType;
import is.moneytracker.util.Message;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;

/**
 * @author Van-Duyet Le
 *
 */
public class AddMoneyFormController implements Initializable {
	// Reference main
	private MoneyTrackerMain mainApp;

	private FXMLLoader loader;
	private AnchorPane anchor;

	public static final String TRANS_INCOME = "Thu nhập";
	public static final String TRANS_OUTCOME = "Chi tiêu";

	public static final String ERROR_PRICE = "Nhập sai số tiền";

	private final ObservableList<TransactionType> form_trans_type_options =
		FXCollections.observableArrayList(
			new TransactionType("income", TRANS_INCOME),
			new TransactionType("outcome", TRANS_OUTCOME)
    );

	private ObservableList<Category> incomeCat;
	private ObservableList<Category> outcomeCat;

	@FXML private ComboBox<TransactionType> form_trans_type;
	@FXML private ComboBox<Category> form_cat;
	@FXML private TextField form_price;
	@FXML private DatePicker form_date;
	@FXML private TextArea form_note;
	@FXML private Button form_submit_btn;

	@FXML private Label form_message_success;

	/**
	 * Constructor
	 */
	public AddMoneyFormController() {
		this.loader = new FXMLLoader(getClass().getResource("view/AddMoneyForm.fxml"));
		this.loader.setController(this);
		try {
			this.setAnchor((AnchorPane) this.loader.load());

		} catch (IOException e) {
			Message.Error(e.getMessage());
			//e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public AddMoneyFormController(MoneyTrackerMain mainApp) {
		this();
		this.mainApp = mainApp;

		org.hibernate.Transaction tx = null;
		Session session = this.getMainApp().getSession();
		try {

			tx = session.beginTransaction();
			List<Category> trans = session.createQuery("FROM Category WHERE type = 'income'").list();
			incomeCat = FXCollections.observableArrayList(trans);
			// tx.commit();

			List<Category> trans2 = session.createQuery("FROM Category WHERE type = 'outcome'").list();
			outcomeCat = FXCollections.observableArrayList(trans2);
			tx.commit();

		} catch (HibernateException e) {
           // if (tx!=null) tx.rollback();

           Message.Error(e.getMessage());
           e.printStackTrace();
        }
	}

	@Override
	public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
		this.form_trans_type.setItems(form_trans_type_options);
		this.form_date.setShowWeekNumbers(false);

		String pattern = "yyyy-MM-dd";
		this.form_date.setConverter(new StringConverter<LocalDate>() {
		     DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);

		     @Override
		     public String toString(LocalDate date) {
		         if (date != null) {
		             return dateFormatter.format(date);
		         } else {
		             return "";
		         }
		     }

		     @Override
		     public LocalDate fromString(String string) {
		         if (string != null && !string.isEmpty()) {
		             return LocalDate.parse(string, dateFormatter);
		         } else {
		             return null;
		         }
		     }
		 });

		// Make price only numberic
		this.form_price.textProperty().addListener(new ChangeListener<String>() {
		    @Override public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		        if (newValue.matches("\\d*")) {
		            // int value = Integer.parseInt(newValue);
		        } else {
		        	form_price.setText(oldValue);
		        }
		    }
		});

		if (this.getMainApp() == null) return;

    }

	@FXML
	private void handleChangeTransType(ActionEvent e) {
		TransactionType currentTrans = this.form_trans_type.getValue();

		if (currentTrans.getId().equals("income")) {
			this.form_cat.setItems(this.incomeCat);
		} else if (currentTrans.getId().equals("outcome")) {
			this.form_cat.setItems(this.outcomeCat);
		}

		// Load categories
		// this.form_cat.setItems();
	}

	@FXML
	public void saveFormData(ActionEvent event) {
		Transaction saver = new Transaction();
		Session session = this.getMainApp().getSession();
        org.hibernate.Transaction tx = null;
        try {
			tx = session.beginTransaction();
			this.form_message_success.setVisible(false);

			int price = 0;
			try {
				price = Integer.parseInt(this.form_price.getText());
			}
			catch (java.lang.NumberFormatException e) {
				Message.Error(AddMoneyFormController.ERROR_PRICE);
				return;
			}
			saver.setPrice(price);
			saver.setNote(this.form_note.getText());
			try {
				System.out.println(this.form_trans_type.getValue().getId().toString());
				saver.setType(this.form_trans_type.getValue().getId());

				if (this.form_trans_type.getValue().getId().equals("outcome"))
					saver.setPrice(-1 * price);
			} catch (NullPointerException e) {
				Message.Error("Vui lòng chọn loại giao dịch");
				return;
			}

			if (this.form_cat.getValue() == null) {
				Message.Error("Vui lòng chọn loại giao dịch");
				return;
			}
			saver.setCategory_id((int) this.form_cat.getValue().getId());

			// TODO
			saver.setUser_id(this.getMainApp().userId);
			saver.setCreated(new Date());
			saver.setStatus("ok");

			// Save
			session.save(saver);
			tx.commit();

			// After save success
			this.form_message_success.setVisible(true);
			this.form_price.setText(null);
			this.form_note.setText(null);

			// Reload main table
			this.getMainApp().getOverviewController().loadTableData();
        } catch (HibernateException e) {
           if (tx!=null) tx.rollback();

           Message.Error(e.getMessage());
           e.printStackTrace();
        }
	}

	public void hideSuccessMessage() {
		this.form_message_success.setVisible(false);
	}

	/**
	 * @return the mainApp
	 */
	public MoneyTrackerMain getMainApp() {
		return mainApp;
	}

	/**
	 * @param mainApp the mainApp to set
	 */
	public void setMainApp(MoneyTrackerMain mainApp) {
		this.mainApp = mainApp;
	}

	/**
	 * @return the anchor
	 */
	public AnchorPane getAnchor() {
		return anchor;
	}

	/**
	 * @param anchor the anchor to set
	 */
	public void setAnchor(AnchorPane anchor) {
		this.anchor = anchor;
	}
}
