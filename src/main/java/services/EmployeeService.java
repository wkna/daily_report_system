package services;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.NoResultException;

import actions.views.EmployeeConverter;
import actions.views.EmployeeView;
import constants.JpaConst;
import models.Employee;
import models.validators.EmployeeValidator;
import utils.EncryptUtil;

public class EmployeeService extends ServiceBase {

    /**
     * @param page ページ数
     * @return 表示するデータのリスト
     */
    public List<EmployeeView> getPerPage(int page) {
        List<Employee> employees = em.createNamedQuery(JpaConst.Q_EMP_GET_ALL, Employee.class)
                .setFirstResult(JpaConst.ROW_PER_PAGE * (page - 1))
                .setMaxResults(JpaConst.ROW_PER_PAGE)
                .getResultList();

        return EmployeeConverter.toViewList(employees);
    }

    /**
     * @return 従業員テーブルのデータの件数
     */
    public long countAll() {
        long empCount = (long) em.createNamedQuery(JpaConst.Q_EMP_COUNT, Long.class)
                .getSingleResult();

        return empCount;
    }

    /**
     * @param code 社員番号
     * @param plainPass パスワード文字列
     * @param pepper pepper文字列
     * @return 取得データのインスタンス 取得できない場合null
     */
    public EmployeeView findOne(String code, String plainPass, String pepper) {
        Employee e = null;
        try {
            String pass = EncryptUtil.getPasswordEncrypt(plainPass, pepper);

            e = em.createNamedQuery(JpaConst.Q_EMP_GET_BY_CODE_AND_PASS, Employee.class)
                    .setParameter(JpaConst.JPQL_PARM_CODE, code)
                    .setParameter(JpaConst.JPQL_PARM_PASSWORD, pass)
                    .getSingleResult();

        } catch (NoResultException ex) {
        }

        return EmployeeConverter.toView(e);

    }

    /**
     * @param id
     * @return 取得データのインスタンス
     */
    public EmployeeView findOne(int id) {
        Employee e = findOneInternal(id);
        return EmployeeConverter.toView(e);
    }

    /**
     * @param code 社員番号
     * @return 該当するデータの件数
     */
    public long countByCode(String code) {

        long employees_count = (long) em.createNamedQuery(JpaConst.Q_EMP_COUNT_RESISTERED_BY_CODE, Long.class)
                .setParameter(JpaConst.JPQL_PARM_CODE, code)
                .getSingleResult();
        return employees_count;
    }

    /**
     * @param ev 画面から入力された従業員の登録内容
     * @param pepper pepper文字列
     * @return バリデーションや登録処理中に発生したエラーのリスト
     */
    public List<String> create(EmployeeView ev, String pepper) {

        String pass = EncryptUtil.getPasswordEncrypt(ev.getPassword(), pepper);
        ev.setPassword(pass);

        LocalDateTime now = LocalDateTime.now();
        ev.setCreatedAt(now);
        ev.setUpdatedAt(now);

        List<String> errors = EmployeeValidator.validate(this, ev, true, true);

        if (errors.size() == 0) {
            create(ev);
        }

        return errors;
    }

    /**
     * @param ev 画面から入力された従業員の登録内容
     * @param pepper pepper文字列
     * @return バリデーションや更新処理中に発生したエラーのリスト
     */
    public List<String> update(EmployeeView ev, String pepper) {

        EmployeeView savedEmp = findOne(ev.getId());

        boolean validateCode = false;
        if (!savedEmp.getCode().equals(ev.getCode())) {

            validateCode = true;
            savedEmp.setCode(ev.getCode());
        }

        boolean validatePass = false;
        if (ev.getPassword() != null && !ev.getPassword().equals("")) {

            validatePass = true;

            savedEmp.setPassword(
                    EncryptUtil.getPasswordEncrypt(ev.getPassword(), pepper));
        }

        savedEmp.setName(ev.getName());
        savedEmp.setAdminFlag(ev.getAdminFlag());

        LocalDateTime today = LocalDateTime.now();
        savedEmp.setUpdatedAt(today);

        List<String> errors = EmployeeValidator.validate(this, savedEmp, validateCode, validatePass);

        if (errors.size() == 0) {
            update(savedEmp);
        }

        return errors;
    }

    /**
     * idを条件に従業員データを論理削除する
     * @param id
     */
    public void destroy(Integer id) {

        EmployeeView savedEmp = findOne(id);

        LocalDateTime today = LocalDateTime.now();
        savedEmp.setUpdatedAt(today);

        savedEmp.setDeleteFlag(JpaConst.EMP_DEL_TRUE);

        update(savedEmp);

    }

    /**
     * @param code 社員番号
     * @param plainPass パスワード
     * @param pepper pepper文字列
     * @return 認証結果を返却す(成功:true 失敗:false)
     */
    public Boolean validateLogin(String code, String plainPass, String pepper) {

        boolean isValidEmployee = false;
        if (code != null && !code.equals("") && plainPass != null && !plainPass.equals("")) {
            EmployeeView ev = findOne(code, plainPass, pepper);

            if (ev != null && ev.getId() != null) {

                isValidEmployee = true;
            }
        }

        return isValidEmployee;
    }

    /**
     * @param id
     * @return 取得データのインスタンス
     */
    private Employee findOneInternal(int id) {
        Employee e = em.find(Employee.class, id);

        return e;
    }

    /**
     * @param ev 従業員データ
     * @return 登録結果(成功:true 失敗:false)
     */
    private void create(EmployeeView ev) {

        em.getTransaction().begin();
        em.persist(EmployeeConverter.toModel(ev));
        em.getTransaction().commit();

    }

    /**
     * @param ev 画面から入力された従業員の登録内容
     */
    private void update(EmployeeView ev) {

        em.getTransaction().begin();
        Employee e = findOneInternal(ev.getId());
        EmployeeConverter.copyViewToModel(e, ev);
        em.getTransaction().commit();

    }

}